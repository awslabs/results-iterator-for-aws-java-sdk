package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicV2GreengrassHelper implements V2GreengrassHelper {
    private final Logger log = LoggerFactory.getLogger(BasicV2GreengrassHelper.class);

    @Inject
    GreengrassIdExtractor greengrassIdExtractor;
    @Inject
    GreengrassClient greengrassClient;

    @Inject
    public BasicV2GreengrassHelper() {
    }

    @Override
    public Stream<GroupInformation> getGroups() {
        return new V2ResultsIterator<GroupInformation>(greengrassClient, ListGroupsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getCoreDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListCoreDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getDeviceDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListDeviceDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityPropertiesById(GreengrassGroupId greengrassGroupId) {
        ListGroupCertificateAuthoritiesRequest listGroupCertificateAuthoritiesRequest = ListGroupCertificateAuthoritiesRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        return new V2ResultsIterator<GroupCertificateAuthorityProperties>(greengrassClient, listGroupCertificateAuthoritiesRequest).stream();
    }

    @Override
    public Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityPropertiesByGroupInformation(GroupInformation groupInformation) {
        return getGroupCertificateAuthorityPropertiesById(ImmutableGreengrassGroupId.builder().groupId(groupInformation.id()).build());
    }

    @Override
    public Predicate<GroupInformation> getGroupNameMatchesPredicate(GreengrassGroupName greengrassGroupName) {
        return groupInformation -> groupInformation.name().equals(greengrassGroupName.getGroupName());
    }

    @Override
    public Predicate<GroupInformation> getGroupIdMatchesPredicate(GreengrassGroupId greengrassGroupId) {
        return groupInformation -> groupInformation.id().equals(greengrassGroupId.getGroupId());
    }

    @Override
    public Predicate<GroupInformation> getGroupNameOrGroupIdMatchesPredicate(String groupNameOrGroupId) {
        return getGroupNameMatchesPredicate(ImmutableGreengrassGroupName.builder().groupName(groupNameOrGroupId).build())
                .or(getGroupIdMatchesPredicate(ImmutableGreengrassGroupId.builder().groupId(groupNameOrGroupId).build()));
    }

    @Override
    public Stream<GroupInformation> getGroupInformationByNameOrId(String groupNameOrGroupId) {
        return getGroups()
                .filter(getGroupNameOrGroupIdMatchesPredicate(groupNameOrGroupId));
    }

    @Override
    public Stream<GroupInformation> getGroupInformationByName(GreengrassGroupName greengrassGroupName) {
        return getGroups()
                .filter(getGroupNameMatchesPredicate(greengrassGroupName));
    }

    @Override
    public Stream<GroupInformation> getGroupInformationById(GreengrassGroupId greengrassGroupId) {
        return getGroups()
                .filter(getGroupIdMatchesPredicate(greengrassGroupId));
    }

    @Override
    public Stream<GreengrassGroupId> getGroupIdByName(GreengrassGroupName greengrassGroupName) {
        return getGroups()
                .filter(getGroupNameMatchesPredicate(greengrassGroupName))
                .map(GroupInformation::id)
                .map(groupId -> ImmutableGreengrassGroupId.builder().groupId(groupId).build());
    }

    @Override
    public Optional<String> getCoreDefinitionIdByName(String coreDefinitionName) {
        return getDefinitionIdByName(getCoreDefinitions(), coreDefinitionName);
    }

    @Override
    public Optional<String> getDeviceDefinitionIdByName(String deviceDefinitionName) {
        return getDefinitionIdByName(getDeviceDefinitions(), deviceDefinitionName);
    }

    private Optional<String> getDefinitionIdByName(Stream<DefinitionInformation> definitionInformationStream, String name) {
        return definitionInformationStream
                // Keep entries with a non-NULL name
                .filter(definitionInformation -> definitionInformation.name() != null)
                // Find entries with matching names
                .filter(definitionInformation -> definitionInformation.name().equals(name))
                // Extract the definition ID
                .map(DefinitionInformation::id)
                .findFirst();
    }

    @Override
    public Optional<GreengrassGroupId> getGroupIdByGroupInformation(GroupInformation groupInformation) {
        return getGroupVersionResponse(groupInformation)
                .map(GetGroupVersionResponse::id)
                .map(groupId -> ImmutableGreengrassGroupId.builder().groupId(groupId).build());
    }

    @Override
    public Optional<GroupVersion> getLatestGroupVersionByGroupInformation(GroupInformation groupInformation) {
        return getGroupVersionResponse(groupInformation)
                .map(GetGroupVersionResponse::definition);
    }

    private Optional<GetGroupVersionResponse> getGroupVersionResponse(GroupInformation groupInformation) {
        GetGroupVersionRequest getGroupVersionRequest = GetGroupVersionRequest.builder()
                .groupId(groupInformation.id())
                .groupVersionId(groupInformation.latestVersion())
                .build();

        // This method throws an exception if the definition does not exist
        return Optional.ofNullable(Try.of(() -> greengrassClient.getGroupVersion(getGroupVersionRequest))
                .getOrNull());
    }

    @Override
    public Optional<List<Function>> getFunctionsByGroupInformation(GroupInformation groupInformation) {
        // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
        return getFunctionDefinitionVersionByGroupInformation(groupInformation)
                .map(FunctionDefinitionVersion::functions)
                .map(ArrayList::new);
    }

    @Override
    public Optional<FunctionDefinitionVersion> getFunctionDefinitionVersionByGroupInformation(GroupInformation groupInformation) {
        Optional<GroupVersion> optionalGroupVersion = getLatestGroupVersionByGroupInformation(groupInformation);

        if (!optionalGroupVersion.isPresent()) {
            return Optional.empty();
        }

        GroupVersion groupVersion = optionalGroupVersion.get();

        String functionDefinitionVersionArn = groupVersion.functionDefinitionVersionArn();

        GetFunctionDefinitionVersionRequest getFunctionDefinitionVersionRequest = GetFunctionDefinitionVersionRequest.builder()
                .functionDefinitionId(greengrassIdExtractor.extractId(functionDefinitionVersionArn))
                .functionDefinitionVersionId(greengrassIdExtractor.extractVersionId(functionDefinitionVersionArn))
                .build();

        // This method throws an exception if the definition does not exist
        GetFunctionDefinitionVersionResponse getFunctionDefinitionVersionResponse = Try.of(() -> greengrassClient.getFunctionDefinitionVersion(getFunctionDefinitionVersionRequest))
                .getOrNull();

        return Optional.ofNullable(getFunctionDefinitionVersionResponse)
                .map(GetFunctionDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<List<Device>> getDevicesByGroupInformation(GroupInformation groupInformation) {
        Optional<GroupVersion> optionalGroupVersion = getLatestGroupVersionByGroupInformation(groupInformation);

        if (!optionalGroupVersion.isPresent()) {
            return Optional.empty();
        }

        GroupVersion groupVersion = optionalGroupVersion.get();

        String deviceDefinitionVersionArn = groupVersion.deviceDefinitionVersionArn();

        GetDeviceDefinitionVersionRequest getDeviceDefinitionVersionRequest = GetDeviceDefinitionVersionRequest.builder()
                .deviceDefinitionId(greengrassIdExtractor.extractId(deviceDefinitionVersionArn))
                .deviceDefinitionVersionId(greengrassIdExtractor.extractVersionId(deviceDefinitionVersionArn))
                .build();

        // This method throws an exception if the definition does not exist
        GetDeviceDefinitionVersionResponse getDeviceDefinitionVersionResponse = Try.of(() -> greengrassClient.getDeviceDefinitionVersion(getDeviceDefinitionVersionRequest))
                .getOrNull();

        // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
        return Optional.ofNullable(getDeviceDefinitionVersionResponse)
                .map(GetDeviceDefinitionVersionResponse::definition)
                .map(DeviceDefinitionVersion::devices)
                .map(ArrayList::new);
    }

    @Override
    public Optional<List<Subscription>> getSubscriptionsByGroupInformation(GroupInformation groupInformation) {
        Optional<GroupVersion> optionalGroupVersion = getLatestGroupVersionByGroupInformation(groupInformation);

        if (!optionalGroupVersion.isPresent()) {
            return Optional.empty();
        }

        GroupVersion groupVersion = optionalGroupVersion.get();

        String subscriptionDefinitionVersionArn = groupVersion.subscriptionDefinitionVersionArn();

        GetSubscriptionDefinitionVersionRequest getSubscriptionDefinitionVersionRequest = GetSubscriptionDefinitionVersionRequest.builder()
                .subscriptionDefinitionId(greengrassIdExtractor.extractId(subscriptionDefinitionVersionArn))
                .subscriptionDefinitionVersionId(greengrassIdExtractor.extractVersionId(subscriptionDefinitionVersionArn))
                .build();

        // This method throws an exception if the definition does not exist
        GetSubscriptionDefinitionVersionResponse getSubscriptionDefinitionVersionResponse = Try.of(() -> greengrassClient.getSubscriptionDefinitionVersion(getSubscriptionDefinitionVersionRequest))
                .getOrNull();

        // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
        return Optional.ofNullable(getSubscriptionDefinitionVersionResponse)
                .map(GetSubscriptionDefinitionVersionResponse::definition)
                .map(SubscriptionDefinitionVersion::subscriptions)
                .map(ArrayList::new);
    }

    @Override
    public Optional<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponseByGroupInformation(GroupInformation groupInformation) {
        List<GroupCertificateAuthorityProperties> groupCertificateAuthorityPropertiesList = getGroupCertificateAuthorityPropertiesByGroupInformation(groupInformation).collect(Collectors.toList());

        if (groupCertificateAuthorityPropertiesList.size() != 1) {
            log.error("Currently we do not support multiple group CAs");
            return Optional.empty();
        }

        GroupCertificateAuthorityProperties groupCertificateAuthorityProperties = groupCertificateAuthorityPropertiesList.get(0);

        GetGroupCertificateAuthorityRequest getGroupCertificateAuthorityRequest = GetGroupCertificateAuthorityRequest.builder()
                .groupId(groupInformation.id())
                .certificateAuthorityId(groupCertificateAuthorityProperties.groupCertificateAuthorityId())
                .build();

        // This method throws an exception if the definition does not exist
        return Optional.ofNullable(Try.of(() -> greengrassClient.getGroupCertificateAuthority(getGroupCertificateAuthorityRequest))
                .getOrNull());
    }

    @Override
    public Optional<FunctionIsolationMode> getDefaultIsolationModeByGroupInformation(GroupInformation groupInformation) {
        Optional<FunctionDefinitionVersion> optionalFunctionDefinitionVersion = getFunctionDefinitionVersionByGroupInformation(groupInformation);

        return optionalFunctionDefinitionVersion
                .map(FunctionDefinitionVersion::defaultConfig)
                .map(FunctionDefaultConfig::execution)
                .map(FunctionDefaultExecutionConfig::isolationMode);
    }

    @Override
    public Optional<CertificateArn> getCoreCertificateArnByGroupVersion(GroupInformation groupInformation) {
        Optional<GroupVersion> optionalGroupVersion = getLatestGroupVersionByGroupInformation(groupInformation);

        if (!optionalGroupVersion.isPresent()) {
            return Optional.empty();
        }

        GroupVersion groupVersion = optionalGroupVersion.get();

        return getCoreCertificateArnByGroupVersion(groupVersion);
    }

    @Override
    public Optional<CertificateArn> getCoreCertificateArnByGroupVersion(GroupVersion groupVersion) {
        String coreDefinitionVersionArn = groupVersion.coreDefinitionVersionArn();
        String coreDefinitionVersionId = greengrassIdExtractor.extractVersionId(coreDefinitionVersionArn);
        String coreDefinitionId = greengrassIdExtractor.extractId(coreDefinitionVersionArn);

        GetCoreDefinitionVersionRequest getCoreDefinitionVersionRequest = GetCoreDefinitionVersionRequest.builder()
                .coreDefinitionVersionId(coreDefinitionVersionId)
                .coreDefinitionId(coreDefinitionId)
                .build();

        // This method throws an exception if the definition does not exist
        GetCoreDefinitionVersionResponse coreDefinitionVersionResponse = Try.of(() -> greengrassClient.getCoreDefinitionVersion(getCoreDefinitionVersionRequest))
                .getOrNull();

        return Optional.ofNullable(coreDefinitionVersionResponse)
                .map(GetCoreDefinitionVersionResponse::definition)
                .map(CoreDefinitionVersion::cores)
                .filter(list -> list.size() != 0)
                .map(list -> list.get(0))
                .map(Core::certificateArn)
                .map(certificateArn -> ImmutableCertificateArn.builder().arn(certificateArn).build());
    }

    @Override
    public boolean groupExistsByName(GreengrassGroupName greengrassGroupName) {
        return getGroupIdByName(greengrassGroupName)
                .findAny()
                .isPresent();
    }

    @Override
    public Optional<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId) {
        return getGroupInformationByNameOrId(groupNameOrGroupId)
                .filter(groupInformation -> groupInformation.latestVersion() != null)
                .findFirst()
                .flatMap(this::getLatestGroupVersionByGroupInformation);
    }
}

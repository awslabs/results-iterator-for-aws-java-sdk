package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.resultsiterator.v2.interfaces.V2ReflectionHelper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
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
    IotIdExtractor iotIdExtractor;
    @Inject
    V2IotHelper v2IotHelper;
    @Inject
    V2ReflectionHelper v2ReflectionHelper;

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
    public Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GreengrassGroupId greengrassGroupId) {
        ListGroupCertificateAuthoritiesRequest listGroupCertificateAuthoritiesRequest = ListGroupCertificateAuthoritiesRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        return new V2ResultsIterator<GroupCertificateAuthorityProperties>(greengrassClient, listGroupCertificateAuthoritiesRequest).stream();
    }

    @Override
    public Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GroupInformation groupInformation) {
        return getGroupCertificateAuthorityProperties(ImmutableGreengrassGroupId.builder().groupId(groupInformation.id()).build());
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
    public Stream<GroupInformation> getGroupInformation(GreengrassGroupName greengrassGroupName) {
        return getGroups()
                .filter(getGroupNameMatchesPredicate(greengrassGroupName));
    }

    @Override
    public Optional<GroupInformation> getGroupInformation(GreengrassGroupId greengrassGroupId) {
        return getGroups()
                .filter(getGroupIdMatchesPredicate(greengrassGroupId))
                .findFirst();
    }

    @Override
    public Stream<GreengrassGroupId> getGroupId(GreengrassGroupName greengrassGroupName) {
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
    public Optional<GreengrassGroupId> getGroupId(GroupInformation groupInformation) {
        return getGroupVersionResponse(groupInformation)
                .map(GetGroupVersionResponse::id)
                .map(groupId -> ImmutableGreengrassGroupId.builder().groupId(groupId).build());
    }

    @Override
    public Optional<GroupVersion> getLatestGroupVersion(GroupInformation groupInformation) {
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
    public Optional<List<Function>> getFunctions(GroupInformation groupInformation) {
        // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
        return getFunctionDefinitionVersion(groupInformation)
                .map(FunctionDefinitionVersion::functions)
                // Put the functions in an array list so the consumer of the list can modify it
                .map(ArrayList::new);
    }

    @Override
    public Optional<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponse(GroupInformation groupInformation) {
        List<GroupCertificateAuthorityProperties> groupCertificateAuthorityPropertiesList = getGroupCertificateAuthorityProperties(groupInformation).collect(Collectors.toList());

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
    public Optional<FunctionIsolationMode> getDefaultIsolationMode(GroupInformation groupInformation) {
        Optional<FunctionDefinitionVersion> optionalFunctionDefinitionVersion = getFunctionDefinitionVersion(groupInformation);

        return optionalFunctionDefinitionVersion
                .map(FunctionDefinitionVersion::defaultConfig)
                .map(FunctionDefaultConfig::execution)
                .map(FunctionDefaultExecutionConfig::isolationMode);
    }

    @Override
    public Optional<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .map(groupVersion -> v2ReflectionHelper.getSingleGreengrassResult(groupVersion.functionDefinitionVersionArn(), "function", GetFunctionDefinitionVersionRequest.class, GetFunctionDefinitionVersionResponse.class))
                .map(GetFunctionDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<List<Device>> getDevices(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .map(groupVersion -> v2ReflectionHelper.getSingleGreengrassResult(groupVersion.deviceDefinitionVersionArn(), "device", GetDeviceDefinitionVersionRequest.class, GetDeviceDefinitionVersionResponse.class))
                .map(GetDeviceDefinitionVersionResponse::definition)
                .map(DeviceDefinitionVersion::devices)
                // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
                .map(ArrayList::new);
    }

    @Override
    public Optional<List<Subscription>> getSubscriptions(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .map(groupVersion -> v2ReflectionHelper.getSingleGreengrassResult(groupVersion.subscriptionDefinitionVersionArn(), "subscription", GetSubscriptionDefinitionVersionRequest.class, GetSubscriptionDefinitionVersionResponse.class))
                .map(GetSubscriptionDefinitionVersionResponse::definition)
                .map(SubscriptionDefinitionVersion::subscriptions)
                // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
                .map(ArrayList::new);
    }

    @Override
    public Optional<CertificateArn> getCoreCertificateArn(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getCoreCertificateArn);
    }

    @Override
    public Optional<CertificateArn> getCoreCertificateArn(GroupVersion groupVersion) {
        return getCoreDefinitionVersion(groupVersion)
                .map(CoreDefinitionVersion::cores)
                .filter(list -> list.size() != 0)
                .map(list -> list.get(0))
                .map(Core::certificateArn)
                .map(certificateArn -> ImmutableCertificateArn.builder().arn(certificateArn).build());
    }

    @Override
    public boolean groupExists(GreengrassGroupName greengrassGroupName) {
        return getGroupId(greengrassGroupName)
                .findAny()
                .isPresent();
    }

    @Override
    public Optional<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId) {
        return getGroupInformationByNameOrId(groupNameOrGroupId)
                .filter(groupInformation -> groupInformation.latestVersion() != null)
                .findFirst()
                .flatMap(this::getLatestGroupVersion);
    }

    @Override
    public Optional<CoreDefinitionVersion> getCoreDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getCoreDefinitionVersion);
    }

    @Override
    public Optional<CoreDefinitionVersion> getCoreDefinitionVersion(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.coreDefinitionVersionArn(), "core", GetCoreDefinitionVersionRequest.class, GetCoreDefinitionVersionResponse.class))
                .map(GetCoreDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getConnectorDefinitionVersion);
    }

    @Override
    public Optional<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.connectorDefinitionVersionArn(), "connector", GetConnectorDefinitionVersionRequest.class, GetConnectorDefinitionVersionResponse.class))
                .map(GetConnectorDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getResourceDefinitionVersion);
    }

    @Override
    public Optional<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.resourceDefinitionVersionArn(), "resource", GetResourceDefinitionVersionRequest.class, GetResourceDefinitionVersionResponse.class))
                .map(GetResourceDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getLoggerDefinitionVersion);
    }

    @Override
    public Optional<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.loggerDefinitionVersionArn(), "logger", GetLoggerDefinitionVersionRequest.class, GetLoggerDefinitionVersionResponse.class))
                .map(GetLoggerDefinitionVersionResponse::definition);
    }

    @Override
    public Stream<Deployment> getDeployments(GroupInformation groupInformation) {
        ListDeploymentsRequest listDeploymentsRequest = ListDeploymentsRequest.builder()
                .groupId(groupInformation.id())
                .build();
        return new V2ResultsIterator<Deployment>(greengrassClient, listDeploymentsRequest).stream();
    }

    @Override
    public Stream<Deployment> getDeployments(GreengrassGroupId greengrassGroupId) {
        return getGroupInformation(greengrassGroupId)
                .map(this::getDeployments)
                .orElse(Stream.empty());
    }

    @Override
    public boolean isGroupImmutable(GreengrassGroupId greengrassGroupId) {
        // Get the group information by group ID
        return getGroupInformation(greengrassGroupId)
                // Get the latest core definition version (flatMap to get rid of Optional<Optional<...>> result
                .flatMap(this::getCoreDefinitionVersion)
                // Get the list of cores
                .map(CoreDefinitionVersion::cores)
                // Convert it to a stream
                .map(Collection::stream)
                // Use an empty stream if no cores exist
                .orElse(Stream.empty())
                .findFirst()
                // Get the thing ARN
                .map(Core::thingArn)
                .map(thingArn -> ImmutableThingArn.builder().arn(thingArn).build())
                // Extract the thing name
                .map(iotIdExtractor::extractThingName)
                // Check if the thing is immutable
                .map(v2IotHelper::isThingImmutable)
                // If the thing wasn't found, return false. Otherwise use the result from the immutability check.
                .orElse(false);
    }
}

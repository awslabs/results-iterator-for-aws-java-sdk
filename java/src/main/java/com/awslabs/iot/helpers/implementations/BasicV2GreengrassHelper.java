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
import java.time.Instant;
import java.util.*;
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
    public Stream<DefinitionInformation> getDeviceDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListDeviceDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getFunctionDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListFunctionDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getCoreDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListCoreDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getConnectorDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListConnectorDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getResourceDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListResourceDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getLoggerDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListLoggerDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getSubscriptionDefinitions() {
        return new V2ResultsIterator<DefinitionInformation>(greengrassClient, ListSubscriptionDefinitionsRequest.class).stream();
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
    public Optional<GroupVersion> getLatestGroupVersion(GreengrassGroupId greengrassGroupId) {
        return getGroupInformation(greengrassGroupId)
                .flatMap(this::getLatestGroupVersion);
    }

    @Override
    public Optional<GroupVersion> getLatestGroupVersion(GroupInformation groupInformation) {
        return getGroupVersionResponse(groupInformation)
                .map(GetGroupVersionResponse::definition);
    }

    private Optional<GetGroupVersionResponse> getGroupVersionResponse(GroupInformation groupInformation) {
        return getGroupVersionResponse(ImmutableGreengrassGroupId.builder().groupId(groupInformation.id()).build(), groupInformation.latestVersion());
    }

    private Optional<GetGroupVersionResponse> getGroupVersionResponse(GreengrassGroupId greengrassGroupId, String versionId) {
        GetGroupVersionRequest getGroupVersionRequest = GetGroupVersionRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .groupVersionId(versionId)
                .build();

        // This method throws an exception if the definition does not exist
        return Optional.ofNullable(Try.of(() -> greengrassClient.getGroupVersion(getGroupVersionRequest))
                .getOrNull());
    }

    @Override
    public Stream<VersionInformation> getVersionInformation(GreengrassGroupId greengrassGroupId) {
        ListGroupVersionsRequest listGroupVersionsRequest = ListGroupVersionsRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        return new V2ResultsIterator<VersionInformation>(greengrassClient, listGroupVersionsRequest).stream();
    }

    @Override
    public Stream<GroupVersion> getGroupVersions(GreengrassGroupId greengrassGroupId) {
        return getVersionInformation(greengrassGroupId)
                .map(versionInformation -> getGroupVersionResponse(greengrassGroupId, versionInformation.version()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(GetGroupVersionResponse::definition);
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
        return Optional.of(Try.of(() -> greengrassClient.getGroupCertificateAuthority(getGroupCertificateAuthorityRequest))
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
                .flatMap(this::getFunctionDefinitionVersion);
    }

    @Override
    public Optional<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupVersion groupVersion) {
        return getFunctionDefinitionVersionResponse(groupVersion)
                .map(GetFunctionDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetFunctionDefinitionVersionResponse> getFunctionDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.functionDefinitionVersionArn(), "function", GetFunctionDefinitionVersionRequest.class, GetFunctionDefinitionVersionResponse.class));
    }

    @Override
    public Optional<CoreDefinitionVersion> getCoreDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getCoreDefinitionVersion);
    }

    @Override
    public Optional<CoreDefinitionVersion> getCoreDefinitionVersion(GroupVersion groupVersion) {
        return getCoreDefinitionVersionResponse(groupVersion)
                .map(GetCoreDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetCoreDefinitionVersionResponse> getCoreDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.coreDefinitionVersionArn(), "core", GetCoreDefinitionVersionRequest.class, GetCoreDefinitionVersionResponse.class));
    }

    @Override
    public Optional<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getConnectorDefinitionVersion);
    }

    @Override
    public Optional<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupVersion groupVersion) {
        return getConnectorDefinitionVersionResponse(groupVersion)
                .map(GetConnectorDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetConnectorDefinitionVersionResponse> getConnectorDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.connectorDefinitionVersionArn(), "connector", GetConnectorDefinitionVersionRequest.class, GetConnectorDefinitionVersionResponse.class));
    }

    @Override
    public Optional<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getResourceDefinitionVersion);
    }

    @Override
    public Optional<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupVersion groupVersion) {
        return getResourceDefinitionVersionResponse(groupVersion)
                .map(GetResourceDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetResourceDefinitionVersionResponse> getResourceDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.resourceDefinitionVersionArn(), "resource", GetResourceDefinitionVersionRequest.class, GetResourceDefinitionVersionResponse.class));
    }

    @Override
    public Optional<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getLoggerDefinitionVersion);
    }

    @Override
    public Optional<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupVersion groupVersion) {
        return getLoggerDefinitionVersionResponse(groupVersion)
                .map(GetLoggerDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetLoggerDefinitionVersionResponse> getLoggerDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.loggerDefinitionVersionArn(), "logger", GetLoggerDefinitionVersionRequest.class, GetLoggerDefinitionVersionResponse.class));
    }

    @Override
    public Optional<SubscriptionDefinitionVersion> getSubscriptionDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getSubscriptionDefinitionVersion);
    }

    @Override
    public Optional<SubscriptionDefinitionVersion> getSubscriptionDefinitionVersion(GroupVersion groupVersion) {
        return getSubscriptionDefinitionVersionResponse(groupVersion)
                .map(GetSubscriptionDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetSubscriptionDefinitionVersionResponse> getSubscriptionDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.subscriptionDefinitionVersionArn(), "subscription", GetSubscriptionDefinitionVersionRequest.class, GetSubscriptionDefinitionVersionResponse.class));
    }

    @Override
    public Optional<DeviceDefinitionVersion> getDeviceDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getDeviceDefinitionVersion);
    }

    @Override
    public Optional<DeviceDefinitionVersion> getDeviceDefinitionVersion(GroupVersion groupVersion) {
        return getDeviceDefinitionVersionResponse(groupVersion)
                .map(GetDeviceDefinitionVersionResponse::definition);
    }

    @Override
    public Optional<GetDeviceDefinitionVersionResponse> getDeviceDefinitionVersionResponse(GroupVersion groupVersion) {
        return Optional.ofNullable(v2ReflectionHelper.getSingleGreengrassResult(groupVersion.deviceDefinitionVersionArn(), "device", GetDeviceDefinitionVersionRequest.class, GetDeviceDefinitionVersionResponse.class));
    }

    @Override
    public Optional<List<Device>> getDevices(GroupInformation groupInformation) {
        return getDeviceDefinitionVersion(groupInformation)
                .map(DeviceDefinitionVersion::devices)
                // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
                .map(ArrayList::new);
    }

    @Override
    public Optional<List<Subscription>> getSubscriptions(GroupInformation groupInformation) {
        return getSubscriptionDefinitionVersion(groupInformation)
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
    public Optional<Deployment> getLatestDeployment(GreengrassGroupId greengrassGroupId) {
        return getGroupInformation(greengrassGroupId)
                .flatMap(this::getLatestDeployment);
    }

    @Override
    public Optional<Deployment> getLatestDeployment(GroupInformation groupInformation) {
        return getDeployments(groupInformation)
                .max(Comparator.comparingLong(deployment -> Instant.parse(deployment.createdAt()).toEpochMilli()));
    }

    @Override
    public Optional<GetDeploymentStatusResponse> getDeploymentStatusResponse(GreengrassGroupId greengrassGroupId, Deployment deployment) {
        GetDeploymentStatusRequest getDeploymentStatusRequest = GetDeploymentStatusRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .deploymentId(deployment.deploymentId())
                .build();

        return Try.of(() -> greengrassClient.getDeploymentStatus(getDeploymentStatusRequest))
                .recover(GreengrassException.class, throwable -> handleDoesNotExistException(throwable, "deployment", deployment.deploymentId()))
                .map(Optional::ofNullable)
                .get();
    }

    private <T> T handleDoesNotExistException(GreengrassException greengrassException, String type, String value) {
        if (!greengrassException.getMessage().contains("does not exist")) {
            throw new RuntimeException(greengrassException);
        }

        log.info("The " + type + " [" + value + "] does not exist");

        return null;
    }

    @Override
    public boolean isGroupImmutable(GreengrassGroupId greengrassGroupId) {
        // Get the group information by group ID
        return getGroupInformation(greengrassGroupId)
                .map(this::isGroupImmutable)
                .orElse(false);
    }

    @Override
    public boolean isGroupImmutable(GroupInformation groupInformation) {
        return Optional.of(groupInformation)
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

    @Override
    public void deleteGroup(GreengrassGroupId greengrassGroupId) {
        ResetDeploymentsRequest resetDeploymentsRequest = ResetDeploymentsRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        greengrassClient.resetDeployments(resetDeploymentsRequest);

        log.info("Reset deployments for group [" + greengrassGroupId.getGroupId() + "]");

        DeleteGroupRequest deleteGroupRequest = DeleteGroupRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        greengrassClient.deleteGroup(deleteGroupRequest);

        log.info("Deleted group [" + greengrassGroupId.getGroupId() + "]");
    }

    @Override
    public void deleteCoreDefinition(DefinitionInformation definitionInformation) {
        DeleteCoreDefinitionRequest deleteCoreDefinitionRequest = DeleteCoreDefinitionRequest.builder()
                .coreDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteCoreDefinition(deleteCoreDefinitionRequest);
    }

    @Override
    public Stream<GetDeviceDefinitionVersionResponse> getImmutableDeviceDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getDeviceDefinitionVersionResponse);
    }

    @Override
    public Stream<GetFunctionDefinitionVersionResponse> getImmutableFunctionDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getFunctionDefinitionVersionResponse);
    }

    @Override
    public Stream<GetCoreDefinitionVersionResponse> getImmutableCoreDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getCoreDefinitionVersionResponse);
    }

    @Override
    public Stream<GetConnectorDefinitionVersionResponse> getImmutableConnectorDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getConnectorDefinitionVersionResponse);
    }

    @Override
    public Stream<GetResourceDefinitionVersionResponse> getImmutableResourceDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getResourceDefinitionVersionResponse);
    }

    @Override
    public Stream<GetLoggerDefinitionVersionResponse> getImmutableLoggerDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getLoggerDefinitionVersionResponse);
    }

    @Override
    public Stream<GetSubscriptionDefinitionVersionResponse> getImmutableSubscriptionDefinitionVersionResponses() {
        return getImmutableDefinitionVersionResponses(this::getSubscriptionDefinitionVersionResponse);
    }


    private <T> Stream<T> getImmutableDefinitionVersionResponses(java.util.function.Function<GroupVersion, Optional<T>> convertFromGroupVersion) {
        return getGroups()
                .filter(this::isGroupImmutable)
                .map(this::getLatestGroupVersion)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(convertFromGroupVersion)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
}

package com.awslabs.iot.helpers.implementations;

import com.awslabs.iam.data.ImmutableRoleArn;
import com.awslabs.iam.data.RoleArn;
import com.awslabs.iam.data.RoleName;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;
import com.awslabs.iot.helpers.interfaces.GreengrassHelper;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.awslabs.resultsiterator.interfaces.ReflectionHelper;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.*;
import software.amazon.awssdk.services.iam.model.Role;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;

public class BasicGreengrassHelper implements GreengrassHelper {
    private final Logger log = LoggerFactory.getLogger(BasicGreengrassHelper.class);

    @Inject
    GreengrassIdExtractor greengrassIdExtractor;
    @Inject
    GreengrassClient greengrassClient;
    @Inject
    IotIdExtractor iotIdExtractor;
    @Inject
    IotHelper iotHelper;
    @Inject
    ReflectionHelper reflectionHelper;
    @Inject
    IamHelper iamHelper;

    @Inject
    public BasicGreengrassHelper() {
    }

    @Override
    public Stream<GroupInformation> getGroups() {
        return new ResultsIterator<GroupInformation>(greengrassClient, ListGroupsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getDeviceDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListDeviceDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getFunctionDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListFunctionDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getCoreDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListCoreDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getConnectorDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListConnectorDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getResourceDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListResourceDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getLoggerDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListLoggerDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<DefinitionInformation> getSubscriptionDefinitions() {
        return new ResultsIterator<DefinitionInformation>(greengrassClient, ListSubscriptionDefinitionsRequest.class).stream();
    }

    @Override
    public Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GreengrassGroupId greengrassGroupId) {
        ListGroupCertificateAuthoritiesRequest listGroupCertificateAuthoritiesRequest = ListGroupCertificateAuthoritiesRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        return new ResultsIterator<GroupCertificateAuthorityProperties>(greengrassClient, listGroupCertificateAuthoritiesRequest).stream();
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
    public Option<GroupInformation> getGroupInformation(GreengrassGroupId greengrassGroupId) {
        return Option.of(getGroups()
                .filter(getGroupIdMatchesPredicate(greengrassGroupId))
                .getOrNull());
    }

    @Override
    public Stream<GreengrassGroupId> getGroupId(GreengrassGroupName greengrassGroupName) {
        return getGroups()
                .filter(getGroupNameMatchesPredicate(greengrassGroupName))
                .map(GroupInformation::id)
                .map(groupId -> ImmutableGreengrassGroupId.builder().groupId(groupId).build());
    }

    @Override
    public Option<String> getCoreDefinitionIdByName(String coreDefinitionName) {
        return getDefinitionIdByName(getCoreDefinitions(), coreDefinitionName);
    }

    @Override
    public Option<String> getDeviceDefinitionIdByName(String deviceDefinitionName) {
        return getDefinitionIdByName(getDeviceDefinitions(), deviceDefinitionName);
    }

    private Option<String> getDefinitionIdByName(Stream<DefinitionInformation> definitionInformationStream, String name) {
        return Option.of(definitionInformationStream
                // Keep entries with a non-NULL name
                .filter(definitionInformation -> definitionInformation.name() != null)
                // Find entries with matching names
                .filter(definitionInformation -> definitionInformation.name().equals(name))
                // Extract the definition ID
                .map(DefinitionInformation::id)
                .getOrNull());
    }

    @Override
    public Option<GreengrassGroupId> getGroupId(GroupInformation groupInformation) {
        return getGroupVersionResponse(groupInformation)
                .map(GetGroupVersionResponse::id)
                .map(groupId -> ImmutableGreengrassGroupId.builder().groupId(groupId).build());
    }

    @Override
    public Option<GroupVersion> getLatestGroupVersion(GreengrassGroupId greengrassGroupId) {
        return getGroupInformation(greengrassGroupId)
                .flatMap(this::getLatestGroupVersion);
    }

    @Override
    public Option<GroupVersion> getLatestGroupVersion(GroupInformation groupInformation) {
        return getGroupVersionResponse(groupInformation)
                .map(GetGroupVersionResponse::definition);
    }

    private Option<GetGroupVersionResponse> getGroupVersionResponse(GroupInformation groupInformation) {
        return getGroupVersionResponse(ImmutableGreengrassGroupId.builder().groupId(groupInformation.id()).build(), groupInformation.latestVersion());
    }

    private Option<GetGroupVersionResponse> getGroupVersionResponse(GreengrassGroupId greengrassGroupId, String versionId) {
        GetGroupVersionRequest getGroupVersionRequest = GetGroupVersionRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .groupVersionId(versionId)
                .build();

        // This method throws an exception if the definition does not exist
        return Option.of(Try.of(() -> greengrassClient.getGroupVersion(getGroupVersionRequest))
                .getOrNull());
    }

    @Override
    public Stream<VersionInformation> getVersionInformation(GreengrassGroupId greengrassGroupId) {
        ListGroupVersionsRequest listGroupVersionsRequest = ListGroupVersionsRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        return new ResultsIterator<VersionInformation>(greengrassClient, listGroupVersionsRequest).stream();
    }

    @Override
    public Stream<GroupVersion> getGroupVersions(GreengrassGroupId greengrassGroupId) {
        return getVersionInformation(greengrassGroupId)
                .map(versionInformation -> getGroupVersionResponse(greengrassGroupId, versionInformation.version()))
                .filter(Option::isDefined)
                .map(Option::get)
                .map(GetGroupVersionResponse::definition);
    }

    @Override
    public Option<List<Function>> getFunctions(GroupInformation groupInformation) {
        // The returned list is an unmodifiable list, copy it to an array list so callers can modify it
        return getFunctionDefinitionVersion(groupInformation)
                .map(FunctionDefinitionVersion::functions)
                .map(List::ofAll);
    }

    @Override
    public Option<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponse(GroupInformation groupInformation) {
        List<GroupCertificateAuthorityProperties> groupCertificateAuthorityPropertiesList = List.ofAll(getGroupCertificateAuthorityProperties(groupInformation));

        if (groupCertificateAuthorityPropertiesList.size() != 1) {
            log.error("Currently we do not support multiple group CAs");
            return Option.none();
        }

        GroupCertificateAuthorityProperties groupCertificateAuthorityProperties = groupCertificateAuthorityPropertiesList.get(0);

        GetGroupCertificateAuthorityRequest getGroupCertificateAuthorityRequest = GetGroupCertificateAuthorityRequest.builder()
                .groupId(groupInformation.id())
                .certificateAuthorityId(groupCertificateAuthorityProperties.groupCertificateAuthorityId())
                .build();

        // This method throws an exception if the definition does not exist
        return Option.of(Try.of(() -> greengrassClient.getGroupCertificateAuthority(getGroupCertificateAuthorityRequest))
                .getOrNull());
    }

    @Override
    public Option<FunctionIsolationMode> getDefaultIsolationMode(GroupInformation groupInformation) {
        Option<FunctionDefinitionVersion> functionDefinitionVersionOption = getFunctionDefinitionVersion(groupInformation);

        return functionDefinitionVersionOption
                .map(FunctionDefinitionVersion::defaultConfig)
                .map(FunctionDefaultConfig::execution)
                .map(FunctionDefaultExecutionConfig::isolationMode);
    }

    @Override
    public Option<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getFunctionDefinitionVersion);
    }

    @Override
    public Option<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupVersion groupVersion) {
        return getFunctionDefinitionVersionResponse(groupVersion)
                .map(GetFunctionDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetFunctionDefinitionVersionResponse> getFunctionDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.functionDefinitionVersionArn(), "function", GetFunctionDefinitionVersionRequest.class, GetFunctionDefinitionVersionResponse.class));
    }

    @Override
    public Option<CoreDefinitionVersion> getCoreDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getCoreDefinitionVersion);
    }

    @Override
    public Option<CoreDefinitionVersion> getCoreDefinitionVersion(GroupVersion groupVersion) {
        return getCoreDefinitionVersionResponse(groupVersion)
                .map(GetCoreDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetCoreDefinitionVersionResponse> getCoreDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.coreDefinitionVersionArn(), "core", GetCoreDefinitionVersionRequest.class, GetCoreDefinitionVersionResponse.class));
    }

    @Override
    public Option<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getConnectorDefinitionVersion);
    }

    @Override
    public Option<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupVersion groupVersion) {
        return getConnectorDefinitionVersionResponse(groupVersion)
                .map(GetConnectorDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetConnectorDefinitionVersionResponse> getConnectorDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.connectorDefinitionVersionArn(), "connector", GetConnectorDefinitionVersionRequest.class, GetConnectorDefinitionVersionResponse.class));
    }

    @Override
    public Option<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getResourceDefinitionVersion);
    }

    @Override
    public Option<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupVersion groupVersion) {
        return getResourceDefinitionVersionResponse(groupVersion)
                .map(GetResourceDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetResourceDefinitionVersionResponse> getResourceDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.resourceDefinitionVersionArn(), "resource", GetResourceDefinitionVersionRequest.class, GetResourceDefinitionVersionResponse.class));
    }

    @Override
    public Option<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getLoggerDefinitionVersion);
    }

    @Override
    public Option<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupVersion groupVersion) {
        return getLoggerDefinitionVersionResponse(groupVersion)
                .map(GetLoggerDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetLoggerDefinitionVersionResponse> getLoggerDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.loggerDefinitionVersionArn(), "logger", GetLoggerDefinitionVersionRequest.class, GetLoggerDefinitionVersionResponse.class));
    }

    @Override
    public Option<SubscriptionDefinitionVersion> getSubscriptionDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getSubscriptionDefinitionVersion);
    }

    @Override
    public Option<SubscriptionDefinitionVersion> getSubscriptionDefinitionVersion(GroupVersion groupVersion) {
        return getSubscriptionDefinitionVersionResponse(groupVersion)
                .map(GetSubscriptionDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetSubscriptionDefinitionVersionResponse> getSubscriptionDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.subscriptionDefinitionVersionArn(), "subscription", GetSubscriptionDefinitionVersionRequest.class, GetSubscriptionDefinitionVersionResponse.class));
    }

    @Override
    public Option<DeviceDefinitionVersion> getDeviceDefinitionVersion(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getDeviceDefinitionVersion);
    }

    @Override
    public Option<DeviceDefinitionVersion> getDeviceDefinitionVersion(GroupVersion groupVersion) {
        return getDeviceDefinitionVersionResponse(groupVersion)
                .map(GetDeviceDefinitionVersionResponse::definition);
    }

    @Override
    public Option<GetDeviceDefinitionVersionResponse> getDeviceDefinitionVersionResponse(GroupVersion groupVersion) {
        return Option.of(reflectionHelper.getSingleGreengrassResult(groupVersion.deviceDefinitionVersionArn(), "device", GetDeviceDefinitionVersionRequest.class, GetDeviceDefinitionVersionResponse.class));
    }

    @Override
    public Option<List<Device>> getDevices(GroupInformation groupInformation) {
        return getDeviceDefinitionVersion(groupInformation)
                .map(DeviceDefinitionVersion::devices)
                .map(List::ofAll);
    }

    @Override
    public Option<List<Subscription>> getSubscriptions(GroupInformation groupInformation) {
        return getSubscriptionDefinitionVersion(groupInformation)
                .map(SubscriptionDefinitionVersion::subscriptions)
                .map(List::ofAll);
    }

    @Override
    public Option<CertificateArn> getCoreCertificateArn(GroupInformation groupInformation) {
        return getLatestGroupVersion(groupInformation)
                .flatMap(this::getCoreCertificateArn);
    }

    @Override
    public Option<CertificateArn> getCoreCertificateArn(GroupVersion groupVersion) {
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
                .nonEmpty();
    }

    @Override
    public Option<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId) {
        return Option.of(getGroupInformationByNameOrId(groupNameOrGroupId)
                .filter(groupInformation -> groupInformation.latestVersion() != null)
                .getOrNull())
                .flatMap(this::getLatestGroupVersion);
    }

    @Override
    public Stream<Deployment> getDeployments(GroupInformation groupInformation) {
        ListDeploymentsRequest listDeploymentsRequest = ListDeploymentsRequest.builder()
                .groupId(groupInformation.id())
                .build();

        return new ResultsIterator<Deployment>(greengrassClient, listDeploymentsRequest).stream();
    }

    @Override
    public Stream<Deployment> getDeployments(GreengrassGroupId greengrassGroupId) {
        return getGroupInformation(greengrassGroupId)
                .map(this::getDeployments)
                .getOrElse(Stream.empty());
    }

    @Override
    public Option<Deployment> getLatestDeployment(GreengrassGroupId greengrassGroupId) {
        return getGroupInformation(greengrassGroupId)
                .flatMap(this::getLatestDeployment);
    }

    @Override
    public Option<Deployment> getLatestDeployment(GroupInformation groupInformation) {
        return Option.of(getDeployments(groupInformation)
                .sortBy(deployment -> Instant.parse(deployment.createdAt()).toEpochMilli())
                .reverse()
                .getOrNull());
    }

    @Override
    public Option<GetDeploymentStatusResponse> getDeploymentStatusResponse(GreengrassGroupId greengrassGroupId, Deployment deployment) {
        GetDeploymentStatusRequest getDeploymentStatusRequest = GetDeploymentStatusRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .deploymentId(deployment.deploymentId())
                .build();

        return Try.of(() -> greengrassClient.getDeploymentStatus(getDeploymentStatusRequest))
                .recover(GreengrassException.class, throwable -> handleDoesNotExistException(throwable, "deployment", deployment.deploymentId()))
                .map(Option::of)
                .get();
    }

    private <T> T handleDoesNotExistException(GreengrassException greengrassException, String type, String value) {
        if (!greengrassException.getMessage().contains("does not exist")) {
            throw new RuntimeException(greengrassException);
        }

        log.debug(String.join("", "The ", type, " [", value, "] does not exist"));

        return null;
    }

    @Override
    public boolean isGroupImmutable(GreengrassGroupId greengrassGroupId) {
        // Get the group information by group ID
        return getGroupInformation(greengrassGroupId)
                .map(this::isGroupImmutable)
                .getOrElse(false);
    }

    @Override
    public boolean isGroupImmutable(GroupInformation groupInformation) {
        return Option.of(groupInformation)
                // Get the latest core definition version (flatMap to get rid of Option<Option<...>> result
                .flatMap(this::getCoreDefinitionVersion)
                // Get the list of cores
                .map(CoreDefinitionVersion::cores)
                // Convert it to a stream
                .map(Stream::ofAll)
                // Use an empty stream if no cores exist
                .getOrElse(Stream.empty())
                // Get the thing ARN
                .map(Core::thingArn)
                .map(thingArn -> ImmutableThingArn.builder().arn(thingArn).build())
                // Extract the thing name
                .map(iotIdExtractor::extractThingName)
                // Check if the thing is immutable
                .map(iotHelper::isThingImmutable)
                // If the thing wasn't found, return false. Otherwise use the result from the immutability check.
                .getOrElse(false);
    }

    @Override
    public void deleteGroup(GreengrassGroupId greengrassGroupId) {
        ResetDeploymentsRequest resetDeploymentsRequest = ResetDeploymentsRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        // Try to reset deployments
        Try.of(() -> greengrassClient.resetDeployments(resetDeploymentsRequest))
                .onSuccess(response -> log.debug(String.join("", "Reset deployments for group [", greengrassGroupId.getGroupId(), "]")))
                .recover(GreengrassException.class, greengrassException -> ignoreIfNotDeployedOrAlreadyReset(greengrassException, greengrassGroupId))
                // Throw all other exceptions
                .get();

        DeleteGroupRequest deleteGroupRequest = DeleteGroupRequest.builder()
                .groupId(greengrassGroupId.getGroupId())
                .build();

        greengrassClient.deleteGroup(deleteGroupRequest);

        log.debug(String.join("", "Deleted group [", greengrassGroupId.getGroupId(), "]"));
    }

    private <T> T ignoreIfNotDeployedOrAlreadyReset(GreengrassException greengrassException, GreengrassGroupId greengrassGroupId) {
        if (greengrassException.getMessage().contains("has not been deployed or has already been reset")) {
            log.debug(String.join("", "Deployments already reset for group [", greengrassGroupId.getGroupId(), "]"));
            return null;
        }

        throw new RuntimeException(greengrassException);
    }

    @Override
    public void deleteDeviceDefinition(DefinitionInformation definitionInformation) {
        DeleteDeviceDefinitionRequest deleteDeviceDefinitionRequest = DeleteDeviceDefinitionRequest.builder()
                .deviceDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteDeviceDefinition(deleteDeviceDefinitionRequest);
    }

    @Override
    public void deleteFunctionDefinition(DefinitionInformation definitionInformation) {
        DeleteFunctionDefinitionRequest deleteFunctionDefinitionRequest = DeleteFunctionDefinitionRequest.builder()
                .functionDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteFunctionDefinition(deleteFunctionDefinitionRequest);
    }

    @Override
    public void deleteCoreDefinition(DefinitionInformation definitionInformation) {
        DeleteCoreDefinitionRequest deleteCoreDefinitionRequest = DeleteCoreDefinitionRequest.builder()
                .coreDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteCoreDefinition(deleteCoreDefinitionRequest);
    }

    @Override
    public void deleteConnectorDefinition(DefinitionInformation definitionInformation) {
        DeleteConnectorDefinitionRequest deleteConnectorDefinitionRequest = DeleteConnectorDefinitionRequest.builder()
                .connectorDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteConnectorDefinition(deleteConnectorDefinitionRequest);
    }

    @Override
    public void deleteResourceDefinition(DefinitionInformation definitionInformation) {
        DeleteResourceDefinitionRequest deleteResourceDefinitionRequest = DeleteResourceDefinitionRequest.builder()
                .resourceDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteResourceDefinition(deleteResourceDefinitionRequest);
    }

    @Override
    public void deleteLoggerDefinition(DefinitionInformation definitionInformation) {
        DeleteLoggerDefinitionRequest deleteLoggerDefinitionRequest = DeleteLoggerDefinitionRequest.builder()
                .loggerDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteLoggerDefinition(deleteLoggerDefinitionRequest);
    }

    @Override
    public void deleteSubscriptionDefinition(DefinitionInformation definitionInformation) {
        DeleteSubscriptionDefinitionRequest deleteSubscriptionDefinitionRequest = DeleteSubscriptionDefinitionRequest.builder()
                .subscriptionDefinitionId(definitionInformation.id())
                .build();

        greengrassClient.deleteSubscriptionDefinition(deleteSubscriptionDefinitionRequest);
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

    @Override
    public CreateSoftwareUpdateJobResponse updateRaspbianCore(ThingArn greengrassCoreThingArn, RoleArn s3UrlSignerRoleArn) {
        CreateSoftwareUpdateJobRequest createSoftwareUpdateJobRequest = CreateSoftwareUpdateJobRequest.builder()
                .updateTargetsArchitecture(UpdateTargetsArchitecture.ARMV7_L)
                .updateTargets(greengrassCoreThingArn.getArn())
                .updateTargetsOperatingSystem(UpdateTargetsOperatingSystem.RASPBIAN)
                .softwareToUpdate(SoftwareToUpdate.CORE)
                .s3UrlSignerRole(s3UrlSignerRoleArn.getArn())
                .updateAgentLogLevel(UpdateAgentLogLevel.WARN)
                .amznClientToken(UUID.randomUUID().toString())
                .build();

        return greengrassClient.createSoftwareUpdateJob(createSoftwareUpdateJobRequest);
    }

    @Override
    public CreateSoftwareUpdateJobResponse updateRaspbianCore(ThingName greengrassCoreThingName, RoleName s3UrlSignerRoleName) {
        Role role = iamHelper.getRole(s3UrlSignerRoleName).get();
        ThingArn thingArn = iotHelper.getThingArn(greengrassCoreThingName).get();

        return updateRaspbianCore(thingArn, ImmutableRoleArn.builder().arn(role.arn()).build());
    }

    @Override
    public Stream<GroupInformation> getNonImmutableGroups() {
        return getGroups()
                // Don't include immutable groups
                .filter(groupInformation -> !isGroupImmutable(groupInformation));
    }

    private <T> Stream<T> getImmutableDefinitionVersionResponses(java.util.function.Function<GroupVersion, Option<T>> convertFromGroupVersion) {
        return getGroups()
                .filter(this::isGroupImmutable)
                .map(this::getLatestGroupVersion)
                .filter(Option::isDefined)
                .map(Option::get)
                .map(convertFromGroupVersion)
                .filter(Option::isDefined)
                .map(Option::get);
    }
}

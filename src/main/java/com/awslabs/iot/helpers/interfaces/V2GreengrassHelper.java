package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iam.data.RoleArn;
import com.awslabs.iam.data.RoleName;
import com.awslabs.iot.data.*;
import io.vavr.control.Option;
import software.amazon.awssdk.services.greengrass.model.*;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface V2GreengrassHelper {
    Stream<GroupInformation> getGroups();

    Stream<DefinitionInformation> getFunctionDefinitions();

    Stream<DefinitionInformation> getCoreDefinitions();

    Stream<DefinitionInformation> getConnectorDefinitions();

    Stream<DefinitionInformation> getDeviceDefinitions();

    Stream<DefinitionInformation> getResourceDefinitions();

    Stream<DefinitionInformation> getLoggerDefinitions();

    Stream<DefinitionInformation> getSubscriptionDefinitions();

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GreengrassGroupId greengrassGroupId);

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GroupInformation groupInformation);

    Predicate<GroupInformation> getGroupNameMatchesPredicate(GreengrassGroupName greengrassGroupName);

    Predicate<GroupInformation> getGroupIdMatchesPredicate(GreengrassGroupId groupId);

    Predicate<GroupInformation> getGroupNameOrGroupIdMatchesPredicate(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformationByNameOrId(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformation(GreengrassGroupName greengrassGroupName);

    Option<GroupInformation> getGroupInformation(GreengrassGroupId greengrassGroupId);

    Stream<GreengrassGroupId> getGroupId(GreengrassGroupName greengrassGroupName);

    Option<String> getCoreDefinitionIdByName(String coreDefinitionName);

    Option<String> getDeviceDefinitionIdByName(String deviceDefinitionName);

    Option<GreengrassGroupId> getGroupId(GroupInformation groupInformation);

    Option<GroupVersion> getLatestGroupVersion(GreengrassGroupId greengrassGroupId);

    Option<GroupVersion> getLatestGroupVersion(GroupInformation groupInformation);

    Stream<VersionInformation> getVersionInformation(GreengrassGroupId greengrassGroupId);

    Stream<GroupVersion> getGroupVersions(GreengrassGroupId greengrassGroupId);

    Option<List<Function>> getFunctions(GroupInformation groupInformation);

    Option<DeviceDefinitionVersion> getDeviceDefinitionVersion(GroupInformation groupInformation);

    Option<DeviceDefinitionVersion> getDeviceDefinitionVersion(GroupVersion groupVersion);

    Option<GetDeviceDefinitionVersionResponse> getDeviceDefinitionVersionResponse(GroupVersion groupVersion);

    Option<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupInformation groupInformation);

    Option<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupVersion groupVersion);

    Option<GetFunctionDefinitionVersionResponse> getFunctionDefinitionVersionResponse(GroupVersion groupVersion);

    Option<CoreDefinitionVersion> getCoreDefinitionVersion(GroupInformation groupInformation);

    Option<CoreDefinitionVersion> getCoreDefinitionVersion(GroupVersion groupVersion);

    Option<GetCoreDefinitionVersionResponse> getCoreDefinitionVersionResponse(GroupVersion groupVersion);

    Option<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupInformation groupInformation);

    Option<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupVersion groupVersion);

    Option<GetConnectorDefinitionVersionResponse> getConnectorDefinitionVersionResponse(GroupVersion groupVersion);

    Option<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupInformation groupInformation);

    Option<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupVersion groupVersion);

    Option<GetResourceDefinitionVersionResponse> getResourceDefinitionVersionResponse(GroupVersion groupVersion);

    Option<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupInformation groupInformation);

    Option<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupVersion groupVersion);

    Option<GetLoggerDefinitionVersionResponse> getLoggerDefinitionVersionResponse(GroupVersion groupVersion);

    Option<SubscriptionDefinitionVersion> getSubscriptionDefinitionVersion(GroupInformation groupInformation);

    Option<SubscriptionDefinitionVersion> getSubscriptionDefinitionVersion(GroupVersion groupVersion);

    Option<GetSubscriptionDefinitionVersionResponse> getSubscriptionDefinitionVersionResponse(GroupVersion groupVersion);

    Option<List<Device>> getDevices(GroupInformation groupInformation);

    Option<List<Subscription>> getSubscriptions(GroupInformation groupInformation);

    Option<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponse(GroupInformation groupInformation);

    Option<FunctionIsolationMode> getDefaultIsolationMode(GroupInformation groupInformation);

    Option<CertificateArn> getCoreCertificateArn(GroupInformation groupInformation);

    Option<CertificateArn> getCoreCertificateArn(GroupVersion groupVersion);

    boolean groupExists(GreengrassGroupName greengrassGroupName);

    Option<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId);

    Stream<Deployment> getDeployments(GroupInformation groupInformation);

    Stream<Deployment> getDeployments(GreengrassGroupId greengrassGroupId);

    Option<Deployment> getLatestDeployment(GreengrassGroupId greengrassGroupId);

    Option<Deployment> getLatestDeployment(GroupInformation groupInformation);

    Option<GetDeploymentStatusResponse> getDeploymentStatusResponse(GreengrassGroupId greengrassGroupId, Deployment deployment);

    boolean isGroupImmutable(GreengrassGroupId greengrassGroupId);

    boolean isGroupImmutable(GroupInformation groupInformation);

    void deleteGroup(GreengrassGroupId greengrassGroupId);

    void deleteDeviceDefinition(DefinitionInformation definitionInformation);

    void deleteFunctionDefinition(DefinitionInformation definitionInformation);

    void deleteConnectorDefinition(DefinitionInformation definitionInformation);

    void deleteResourceDefinition(DefinitionInformation definitionInformation);

    void deleteLoggerDefinition(DefinitionInformation definitionInformation);

    void deleteCoreDefinition(DefinitionInformation definitionInformation);

    Stream<GetCoreDefinitionVersionResponse> getImmutableCoreDefinitionVersionResponses();

    Stream<GetConnectorDefinitionVersionResponse> getImmutableConnectorDefinitionVersionResponses();

    void deleteSubscriptionDefinition(DefinitionInformation definitionInformation);

    Stream<GetDeviceDefinitionVersionResponse> getImmutableDeviceDefinitionVersionResponses();

    Stream<GetFunctionDefinitionVersionResponse> getImmutableFunctionDefinitionVersionResponses();

    Stream<GetResourceDefinitionVersionResponse> getImmutableResourceDefinitionVersionResponses();

    Stream<GetLoggerDefinitionVersionResponse> getImmutableLoggerDefinitionVersionResponses();

    Stream<GetSubscriptionDefinitionVersionResponse> getImmutableSubscriptionDefinitionVersionResponses();

    CreateSoftwareUpdateJobResponse updateRaspbianCore(ThingArn greengrassCoreThingArn, RoleArn s3UrlSignerRoleArn);

    CreateSoftwareUpdateJobResponse updateRaspbianCore(ThingName greengrassCoreThingName, RoleName s3UrlSignerRoleName);

    Stream<GroupInformation> getNonImmutableGroups();
}

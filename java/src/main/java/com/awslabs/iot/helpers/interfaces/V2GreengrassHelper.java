package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iot.data.CertificateArn;
import com.awslabs.iot.data.GreengrassGroupId;
import com.awslabs.iot.data.GreengrassGroupName;
import software.amazon.awssdk.services.greengrass.model.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface V2GreengrassHelper {
    Stream<GroupInformation> getGroups();

    Stream<DefinitionInformation> getCoreDefinitions();

    Stream<DefinitionInformation> getDeviceDefinitions();

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GreengrassGroupId greengrassGroupId);

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityProperties(GroupInformation groupInformation);

    Predicate<GroupInformation> getGroupNameMatchesPredicate(GreengrassGroupName greengrassGroupName);

    Predicate<GroupInformation> getGroupIdMatchesPredicate(GreengrassGroupId groupId);

    Predicate<GroupInformation> getGroupNameOrGroupIdMatchesPredicate(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformationByNameOrId(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformation(GreengrassGroupName greengrassGroupName);

    Optional<GroupInformation> getGroupInformation(GreengrassGroupId greengrassGroupId);

    Stream<GreengrassGroupId> getGroupId(GreengrassGroupName greengrassGroupName);

    Optional<String> getCoreDefinitionIdByName(String coreDefinitionName);

    Optional<String> getDeviceDefinitionIdByName(String deviceDefinitionName);

    Optional<GreengrassGroupId> getGroupId(GroupInformation groupInformation);

    Optional<GroupVersion> getLatestGroupVersion(GreengrassGroupId greengrassGroupId);

    Optional<GroupVersion> getLatestGroupVersion(GroupInformation groupInformation);

    Stream<VersionInformation> getVersionInformation(GreengrassGroupId greengrassGroupId);

    Stream<GroupVersion> getGroupVersions(GreengrassGroupId greengrassGroupId);

    Optional<List<Function>> getFunctions(GroupInformation groupInformation);

    Optional<FunctionDefinitionVersion> getFunctionDefinitionVersion(GroupInformation groupInformation);

    Optional<List<Device>> getDevices(GroupInformation groupInformation);

    Optional<List<Subscription>> getSubscriptions(GroupInformation groupInformation);

    Optional<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponse(GroupInformation groupInformation);

    Optional<FunctionIsolationMode> getDefaultIsolationMode(GroupInformation groupInformation);

    Optional<CertificateArn> getCoreCertificateArn(GroupInformation groupInformation);

    Optional<CertificateArn> getCoreCertificateArn(GroupVersion groupVersion);

    boolean groupExists(GreengrassGroupName greengrassGroupName);

    Optional<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId);

    Optional<CoreDefinitionVersion> getCoreDefinitionVersion(GroupInformation groupInformation);

    Optional<CoreDefinitionVersion> getCoreDefinitionVersion(GroupVersion groupVersion);

    Optional<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupInformation groupInformation);

    Optional<ConnectorDefinitionVersion> getConnectorDefinitionVersion(GroupVersion groupVersion);

    Optional<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupInformation groupInformation);

    Optional<ResourceDefinitionVersion> getResourceDefinitionVersion(GroupVersion groupVersion);

    Optional<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupInformation groupInformation);

    Optional<LoggerDefinitionVersion> getLoggerDefinitionVersion(GroupVersion groupVersion);

    Stream<Deployment> getDeployments(GroupInformation groupInformation);

    Stream<Deployment> getDeployments(GreengrassGroupId greengrassGroupId);

    boolean isGroupImmutable(GreengrassGroupId greengrassGroupId);
}

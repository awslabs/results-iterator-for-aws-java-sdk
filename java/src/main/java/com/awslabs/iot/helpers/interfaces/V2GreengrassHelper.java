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

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityPropertiesById(GreengrassGroupId greengrassGroupId);

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityPropertiesByGroupInformation(GroupInformation groupInformation);

    Predicate<GroupInformation> getGroupNameMatchesPredicate(GreengrassGroupName greengrassGroupName);

    Predicate<GroupInformation> getGroupIdMatchesPredicate(GreengrassGroupId groupId);

    Predicate<GroupInformation> getGroupNameOrGroupIdMatchesPredicate(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformationByNameOrId(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformationByName(GreengrassGroupName greengrassGroupName);

    Stream<GroupInformation> getGroupInformationById(GreengrassGroupId greengrassGroupId);

    Stream<GreengrassGroupId> getGroupIdByName(GreengrassGroupName greengrassGroupName);

    Optional<String> getCoreDefinitionIdByName(String coreDefinitionName);

    Optional<String> getDeviceDefinitionIdByName(String deviceDefinitionName);

    Optional<GreengrassGroupId> getGroupIdByGroupInformation(GroupInformation groupInformation);

    Optional<GroupVersion> getLatestGroupVersionByGroupInformation(GroupInformation groupInformation);

    Optional<List<Function>> getFunctionsByGroupInformation(GroupInformation groupInformation);

    Optional<FunctionDefinitionVersion> getFunctionDefinitionVersionByGroupInformation(GroupInformation groupInformation);

    Optional<List<Device>> getDevicesByGroupInformation(GroupInformation groupInformation);

    Optional<List<Subscription>> getSubscriptionsByGroupInformation(GroupInformation groupInformation);

    Optional<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponseByGroupInformation(GroupInformation groupInformation);

    Optional<FunctionIsolationMode> getDefaultIsolationModeByGroupInformation(GroupInformation groupInformation);

    Optional<CertificateArn> getCoreCertificateArnByGroupInformation(GroupInformation groupInformation);

    Optional<CertificateArn> getCoreCertificateArnByGroupVersion(GroupVersion groupVersion);

    boolean groupExistsByName(GreengrassGroupName greengrassGroupName);

    Optional<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId);
}

package com.awslabs.iot.helpers.interfaces;

import software.amazon.awssdk.services.greengrass.model.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface V2GreengrassHelper {
    Stream<GroupInformation> getGroups();

    Stream<DefinitionInformation> getCoreDefinitions();

    Stream<DefinitionInformation> getDeviceDefinitions();

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityPropertiesById(String groupId);

    Stream<GroupCertificateAuthorityProperties> getGroupCertificateAuthorityPropertiesByGroupInformation(GroupInformation groupInformation);

    Predicate<GroupInformation> getGroupNameMatchesPredicate(String groupName);

    Predicate<GroupInformation> getGroupIdMatchesPredicate(String groupId);

    Predicate<GroupInformation> getGroupNameOrGroupIdMatchesPredicate(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformationByNameOrId(String groupNameOrGroupId);

    Stream<GroupInformation> getGroupInformationByName(String groupName);

    Stream<GroupInformation> getGroupInformationById(String groupId);

    Stream<String> getGroupIdByName(String groupName);

    Optional<String> getCoreDefinitionIdByName(String coreDefinitionName);

    Optional<String> getDeviceDefinitionIdByName(String deviceDefinitionName);

    Optional<String> getGroupIdByGroupInformation(GroupInformation groupInformation);

    Optional<GroupVersion> getLatestGroupVersionByGroupInformation(GroupInformation groupInformation);

    Optional<List<Function>> getFunctionsByGroupInformation(GroupInformation groupInformation);

    Optional<FunctionDefinitionVersion> getFunctionDefinitionVersionByGroupInformation(GroupInformation groupInformation);

    Optional<List<Device>> getDevicesByGroupInformation(GroupInformation groupInformation);

    Optional<List<Subscription>> getSubscriptionsByGroupInformation(GroupInformation groupInformation);

    Optional<GetGroupCertificateAuthorityResponse> getGroupCertificateAuthorityResponseByGroupInformation(GroupInformation groupInformation);

    Optional<FunctionIsolationMode> getDefaultIsolationModeByGroupInformation(GroupInformation groupInformation);

    Optional<String> getCoreCertificateArnByGroupInformation(GroupInformation groupInformation);

    boolean groupExistsByName(String groupName);

    Optional<GroupVersion> getLatestGroupVersionByNameOrId(String groupNameOrGroupId);
}

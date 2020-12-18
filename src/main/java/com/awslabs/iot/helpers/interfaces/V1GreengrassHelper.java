package com.awslabs.iot.helpers.interfaces;

import com.amazonaws.services.greengrass.model.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface V1GreengrassHelper {
    Stream<GroupInformation> listGroups();

    Stream<String> listGroupArns();

    Stream<String> listGroupIds();

    Map<String, VersionInformation> listLatestGroupVersions();

    Map<String, VersionInformation> listLatestImmutableGroupVersions();

    Stream<DefinitionInformation> listNonImmutableCoreDefinitionInformation();

    Stream<DefinitionInformation> listNonImmutableDeviceDefinitionInformation();

    Stream<DefinitionInformation> listNonImmutableFunctionDefinitionInformation();

    Stream<DefinitionInformation> listNonImmutableLoggerDefinitionInformation();

    Stream<DefinitionInformation> listNonImmutableResourceDefinitionInformation();

    Stream<DefinitionInformation> listNonImmutableConnectorDefinitionInformation();

    Stream<DefinitionInformation> listNonImmutableSubscriptionDefinitionInformation();

    Stream<VersionInformation> listGroupVersions(String groupId);

    Optional<VersionInformation> getLatestGroupVersion(String groupId);

    Stream<Deployment> listDeployments(String groupId);

    Stream<String> listDeploymentIds(String groupId);

    Optional<Deployment> getLatestDeployment(String groupId);

    String getDeploymentStatus(String groupId, String deploymentId);

    String getCoreDefinitionVersionArn(String groupId, VersionInformation versionInformation);

    String getConnectorDefinitionVersionArn(String groupId, VersionInformation versionInformation);

    GetCoreDefinitionResult getCoreDefinition(String groupId, VersionInformation versionInformation);

    GetGroupVersionResult getGroupVersion(String groupId, VersionInformation versionInformation);

    String getFunctionDefinitionVersionArn(String groupId, VersionInformation versionInformation);

    String getLoggerDefinitionVersionArn(String groupId, VersionInformation versionInformation);

    GetFunctionDefinitionResult getFunctionDefinition(String groupId, VersionInformation versionInformation);

    GetFunctionDefinitionVersionResult getFunctionDefinitionVersion(String groupId, VersionInformation versionInformation);

    String getResourceDefinitionVersionArn(String groupId, VersionInformation versionInformation);

    String getSubscriptionDefinitionVersionArn(String groupId, VersionInformation versionInformation);

    GetSubscriptionDefinitionResult getSubscriptionDefinition(String groupId, VersionInformation versionInformation);

    GetCoreDefinitionVersionResult getCoreDefinitionVersion(String groupId, VersionInformation versionInformation);

    GetSubscriptionDefinitionVersionResult getSubscriptionDefinitionVersion(String groupId, VersionInformation versionInformation);

    void deleteGroup(String groupId);

    GetDeviceDefinitionVersionResult getDeviceDefinitionVersion(String groupId, VersionInformation versionInformation);

    GetConnectorDefinitionVersionResult getConnectorDefinitionVersion(String groupId, VersionInformation versionInformation);

    Stream<DefinitionInformation> listCoreDefinitions();

    void deleteCoreDefinition(DefinitionInformation definitionInformation);

    Stream<DefinitionInformation> listFunctionDefinitions();

    void deleteFunctionDefinition(DefinitionInformation definitionInformation);

    Stream<DefinitionInformation> listSubscriptionDefinitions();

    void deleteSubscriptionDefinition(DefinitionInformation definitionInformation);

    Stream<DefinitionInformation> listDeviceDefinitions();

    void deleteDeviceDefinition(DefinitionInformation definitionInformation);

    GetLoggerDefinitionVersionResult getLoggerDefinitionVersion(String groupId, VersionInformation versionInformation);

    void deleteLoggerDefinition(DefinitionInformation definitionInformation);

    Stream<DefinitionInformation> listLoggerDefinitions();

    GetResourceDefinitionVersionResult getResourceDefinitionVersion(String groupId, VersionInformation versionInformation);

    Stream<DefinitionInformation> listResourceDefinitions();

    Stream<DefinitionInformation> listConnectorDefinitions();

    void deleteResourceDefinition(DefinitionInformation definitionInformation);

    void deleteConnectorDefinition(DefinitionInformation definitionInformation);

    boolean groupExists(String groupId);

    boolean isGroupImmutable(String groupId);
}

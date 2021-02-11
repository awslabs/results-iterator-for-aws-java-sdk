package com.awslabs.iot.helpers.interfaces;

import com.amazonaws.services.greengrass.model.*;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

public interface V1GreengrassHelper {
    Stream<GroupInformation> listGroups();

    io.vavr.collection.Stream<String> listGroupArns();

    io.vavr.collection.Stream<String> listGroupIds();

    Map<String, VersionInformation> listLatestGroupVersions();

    Map<String, VersionInformation> listLatestImmutableGroupVersions();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableCoreDefinitionInformation();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableDeviceDefinitionInformation();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableFunctionDefinitionInformation();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableLoggerDefinitionInformation();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableResourceDefinitionInformation();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableConnectorDefinitionInformation();

    io.vavr.collection.Stream<DefinitionInformation> listNonImmutableSubscriptionDefinitionInformation();

    io.vavr.collection.Stream<VersionInformation> listGroupVersions(String groupId);

    Option<VersionInformation> getLatestGroupVersion(String groupId);

    io.vavr.collection.Stream<Deployment> listDeployments(String groupId);

    io.vavr.collection.Stream<String> listDeploymentIds(String groupId);

    Option<Deployment> getLatestDeployment(String groupId);

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

    io.vavr.collection.Stream<DefinitionInformation> listCoreDefinitions();

    void deleteCoreDefinition(DefinitionInformation definitionInformation);

    io.vavr.collection.Stream<DefinitionInformation> listFunctionDefinitions();

    void deleteFunctionDefinition(DefinitionInformation definitionInformation);

    io.vavr.collection.Stream<DefinitionInformation> listSubscriptionDefinitions();

    void deleteSubscriptionDefinition(DefinitionInformation definitionInformation);

    io.vavr.collection.Stream<DefinitionInformation> listDeviceDefinitions();

    void deleteDeviceDefinition(DefinitionInformation definitionInformation);

    GetLoggerDefinitionVersionResult getLoggerDefinitionVersion(String groupId, VersionInformation versionInformation);

    void deleteLoggerDefinition(DefinitionInformation definitionInformation);

    io.vavr.collection.Stream<DefinitionInformation> listLoggerDefinitions();

    GetResourceDefinitionVersionResult getResourceDefinitionVersion(String groupId, VersionInformation versionInformation);

    io.vavr.collection.Stream<DefinitionInformation> listResourceDefinitions();

    io.vavr.collection.Stream<DefinitionInformation> listConnectorDefinitions();

    void deleteResourceDefinition(DefinitionInformation definitionInformation);

    void deleteConnectorDefinition(DefinitionInformation definitionInformation);

    boolean groupExists(String groupId);

    boolean isGroupImmutable(String groupId);
}

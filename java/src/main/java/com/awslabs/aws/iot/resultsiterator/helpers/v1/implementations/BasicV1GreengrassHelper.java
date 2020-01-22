package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.greengrass.AWSGreengrassClient;
import com.amazonaws.services.greengrass.model.*;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1GreengrassHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1ThingHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicV1GreengrassHelper implements V1GreengrassHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1GreengrassHelper.class);
    @Inject
    AWSGreengrassClient awsGreengrassClient;
    @Inject
    GreengrassIdExtractor greengrassIdExtractor;
    @Inject
    Provider<V1ThingHelper> thingHelperProvider;

    @Inject
    public BasicV1GreengrassHelper() {
    }

    @Override
    public Stream<GroupInformation> listGroups() {
        // Return the list sorted so overlapping names can be found easily
        return sortGroupInformation(new V1ResultsIterator<GroupInformation>(awsGreengrassClient, ListGroupsRequest.class).stream());
    }

    @Override
    public Stream<String> listGroupArns() {
        return mapGroupInfo(String.class, GroupInformation::getArn);
    }

    @Override
    public Stream<String> listGroupIds() {
        return mapGroupInfo(String.class, GroupInformation::getId);
    }

    @Override
    public Map<String, VersionInformation> listLatestGroupVersions() {
        return listGroups()
                .map(group -> new AbstractMap.SimpleEntry<>(group.getId(), getLatestGroupVersion(group.getId())))
                .filter(entry -> entry.getValue().isPresent())
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Override
    public Map<String, VersionInformation> listLatestImmutableGroupVersions() {
        return listGroups()
                .filter(group -> isGroupImmutable(group.getId()))
                .map(group -> new AbstractMap.SimpleEntry<>(group.getId(), getLatestGroupVersion(group.getId())))
                .filter(entry -> entry.getValue().isPresent())
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableCoreDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listCoreDefinitions, this::listLatestImmutableCoreDefinitionVersionArns);
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableDeviceDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listDeviceDefinitions, this::listLatestImmutableDeviceDefinitionVersionArns);
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableFunctionDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listFunctionDefinitions, this::listLatestImmutableFunctionDefinitionVersionArns);
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableLoggerDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listLoggerDefinitions, this::listLatestImmutableLoggerDefinitionVersionArns);
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableResourceDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listResourceDefinitions, this::listLatestImmutableResourceDefinitionVersionArns);
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableConnectorDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listConnectorDefinitions, this::listLatestImmutableConnectorDefinitionVersionArns);
    }

    @Override
    public Stream<DefinitionInformation> listNonImmutableSubscriptionDefinitionInformation() {
        return listNonImmutableDefinitionInformation(this::listSubscriptionDefinitions, this::listLatestImmutableSubscriptionDefinitionVersionArns);
    }

    private Stream<DefinitionInformation> listNonImmutableDefinitionInformation(Supplier<Stream<DefinitionInformation>> definitionInformationSupplier,
                                                                                Supplier<Stream<String>> immutableDefinitionVersionArnSupplier) {
        Stream<DefinitionInformation> definitionInformationStream = definitionInformationSupplier.get();
        List<String> latestCoreDefinitions = immutableDefinitionVersionArnSupplier.get().collect(Collectors.toList());

        // Remove all definitions that are immutable versions
        return definitionInformationStream
                .filter(d -> !latestCoreDefinitions.contains(d.getLatestVersionArn()));
    }

    private Stream<String> listLatestImmutableCoreDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getCoreDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetCoreDefinitionVersionResult::getArn)
                .distinct();
    }

    private Stream<String> listLatestImmutableDeviceDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getDeviceDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetDeviceDefinitionVersionResult::getArn)
                .distinct();
    }

    private Stream<String> listLatestImmutableFunctionDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getFunctionDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetFunctionDefinitionVersionResult::getArn)
                .distinct();
    }

    private Stream<String> listLatestImmutableLoggerDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getLoggerDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetLoggerDefinitionVersionResult::getArn)
                .distinct();
    }

    private Stream<String> listLatestImmutableResourceDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getResourceDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetResourceDefinitionVersionResult::getArn)
                .distinct();
    }

    private Stream<String> listLatestImmutableConnectorDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getConnectorDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetConnectorDefinitionVersionResult::getArn)
                .distinct();
    }

    private Stream<String> listLatestImmutableSubscriptionDefinitionVersionArns() {
        return listLatestImmutableGroupVersions().entrySet().stream()
                .map(e -> getSubscriptionDefinitionVersion(e.getKey(), e.getValue()))
                .filter(Objects::nonNull)
                .map(GetSubscriptionDefinitionVersionResult::getArn)
                .distinct();
    }

    private <V> Stream<V> mapGroupInfo(Class<V> returnType, Function<? super GroupInformation, V> x) {
        return listGroups()
                .map(x);
    }

    @Override
    public Stream<VersionInformation> listGroupVersions(String groupId) {
        ListGroupVersionsRequest listGroupVersionsRequest = new ListGroupVersionsRequest().withGroupId(groupId);

        // Return the list sorted so we can easily find the latest version
        return sortGroupVersionInformation(new V1ResultsIterator<VersionInformation>(awsGreengrassClient, listGroupVersionsRequest).stream());
    }

    @Override
    public Optional<VersionInformation> getLatestGroupVersion(String groupId) {
        // Get the last group version or return NULL if there aren't any deployments
        return listGroupVersions(groupId)
                .min(Collections.reverseOrder(Comparator.comparingLong(versionInformation -> Instant.parse(versionInformation.getCreationTimestamp()).toEpochMilli())));
    }

    @Override
    public Stream<Deployment> listDeployments(String groupId) {
        ListDeploymentsRequest listDeploymentsRequest = new ListDeploymentsRequest()
                .withGroupId(groupId);

        // Return the list sorted so we can easily find the latest deployment
        return sortDeployments(new V1ResultsIterator<Deployment>(awsGreengrassClient, listDeploymentsRequest).stream());
    }

    @Override
    public Stream<String> listDeploymentIds(String groupId) {
        return listDeployments(groupId)
                .map(Deployment::getDeploymentId);
    }

    @Override
    public Optional<Deployment> getLatestDeployment(String groupId) {
        // Get the last deployment or return NULL if there aren't any deployments
        return listDeployments(groupId)
                .min(Collections.reverseOrder(Comparator.comparingLong(deployment -> Instant.parse(deployment.getCreatedAt()).toEpochMilli())));
    }

    @Override
    public String getDeploymentStatus(String groupId, String deploymentId) {
        GetDeploymentStatusRequest getDeploymentStatusRequest = new GetDeploymentStatusRequest()
                .withGroupId(groupId)
                .withDeploymentId(deploymentId);

        GetDeploymentStatusResult getDeploymentStatusResult = awsGreengrassClient.getDeploymentStatus(getDeploymentStatusRequest);

        if (getDeploymentStatusResult == null) {
            return null;
        }

        return getDeploymentStatusResult.getDeploymentStatus();
    }

    @Override
    public String getCoreDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getCoreDefinitionVersionArn();
    }

    @Override
    public String getConnectorDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getConnectorDefinitionVersionArn();
    }

    @Override
    public GetCoreDefinitionResult getCoreDefinition(String groupId, VersionInformation versionInformation) {
        String coreDefinitionVersionArn = getCoreDefinitionVersionArn(groupId, versionInformation);

        GetCoreDefinitionRequest getCoreDefinitionRequest = new GetCoreDefinitionRequest()
                .withCoreDefinitionId(greengrassIdExtractor.extractId(coreDefinitionVersionArn));
        return awsGreengrassClient.getCoreDefinition(getCoreDefinitionRequest);
    }

    @Override
    public GetCoreDefinitionVersionResult getCoreDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String coreDefinitionVersionArn = getCoreDefinitionVersionArn(groupId, versionInformation);

        return getCoreDefinitionVersion(greengrassIdExtractor.extractId(coreDefinitionVersionArn), greengrassIdExtractor.extractVersionId(coreDefinitionVersionArn));
    }

    private GetCoreDefinitionVersionResult getCoreDefinitionVersion(String coreDefinitionId, String coreDefinitionVersionId) {
        GetCoreDefinitionVersionRequest getCoreDefinitionVersionRequest = new GetCoreDefinitionVersionRequest()
                .withCoreDefinitionId(coreDefinitionId)
                .withCoreDefinitionVersionId(coreDefinitionVersionId);
        return awsGreengrassClient.getCoreDefinitionVersion(getCoreDefinitionVersionRequest);
    }

    @Override
    public GetGroupVersionResult getGroupVersion(String groupId, VersionInformation versionInformation) {
        GetGroupVersionRequest getGroupVersionRequest = new GetGroupVersionRequest()
                .withGroupId(groupId)
                .withGroupVersionId(versionInformation.getVersion());

        return awsGreengrassClient.getGroupVersion(getGroupVersionRequest);
    }

    @Override
    public String getFunctionDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getFunctionDefinitionVersionArn();
    }

    @Override
    public String getLoggerDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getLoggerDefinitionVersionArn();
    }

    @Override
    public GetFunctionDefinitionResult getFunctionDefinition(String groupId, VersionInformation versionInformation) {
        String functionDefinitionVersionArn = getFunctionDefinitionVersionArn(groupId, versionInformation);

        GetFunctionDefinitionRequest getFunctionDefinitionRequest = new GetFunctionDefinitionRequest()
                .withFunctionDefinitionId(greengrassIdExtractor.extractId(functionDefinitionVersionArn));
        return awsGreengrassClient.getFunctionDefinition(getFunctionDefinitionRequest);
    }

    @Override
    public GetFunctionDefinitionVersionResult getFunctionDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String functionDefinitionVersionArn = getFunctionDefinitionVersionArn(groupId, versionInformation);

        if (functionDefinitionVersionArn == null) {
            return null;
        }

        GetFunctionDefinitionVersionRequest getFunctionDefinitionVersionRequest = new GetFunctionDefinitionVersionRequest()
                .withFunctionDefinitionId(greengrassIdExtractor.extractId(functionDefinitionVersionArn))
                .withFunctionDefinitionVersionId(greengrassIdExtractor.extractVersionId(functionDefinitionVersionArn));
        return awsGreengrassClient.getFunctionDefinitionVersion(getFunctionDefinitionVersionRequest);
    }

    @Override
    public GetLoggerDefinitionVersionResult getLoggerDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String loggerDefinitionVersionArn = getLoggerDefinitionVersionArn(groupId, versionInformation);

        if (loggerDefinitionVersionArn == null) {
            return null;
        }

        GetLoggerDefinitionVersionRequest getLoggerDefinitionVersionRequest = new GetLoggerDefinitionVersionRequest()
                .withLoggerDefinitionId(greengrassIdExtractor.extractId(loggerDefinitionVersionArn))
                .withLoggerDefinitionVersionId(greengrassIdExtractor.extractVersionId(loggerDefinitionVersionArn));
        return awsGreengrassClient.getLoggerDefinitionVersion(getLoggerDefinitionVersionRequest);
    }

    @Override
    public void deleteLoggerDefinition(DefinitionInformation definitionInformation) {
        DeleteLoggerDefinitionRequest deleteLoggerDefinitionRequest = new DeleteLoggerDefinitionRequest()
                .withLoggerDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteLoggerDefinition(deleteLoggerDefinitionRequest);
    }

    @Override
    public Stream<DefinitionInformation> listLoggerDefinitions() {
        ListLoggerDefinitionsRequest listLoggerDefinitionsRequest = new ListLoggerDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listLoggerDefinitionsRequest).stream();
    }

    @Override
    public GetResourceDefinitionVersionResult getResourceDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String resourceDefinitionVersionArn = getResourceDefinitionVersionArn(groupId, versionInformation);

        if (resourceDefinitionVersionArn == null) {
            return null;
        }

        GetResourceDefinitionVersionRequest getResourceDefinitionVersionRequest = new GetResourceDefinitionVersionRequest()
                .withResourceDefinitionId(greengrassIdExtractor.extractId(resourceDefinitionVersionArn))
                .withResourceDefinitionVersionId(greengrassIdExtractor.extractVersionId(resourceDefinitionVersionArn));
        return awsGreengrassClient.getResourceDefinitionVersion(getResourceDefinitionVersionRequest);
    }

    @Override
    public Stream<DefinitionInformation> listResourceDefinitions() {
        ListResourceDefinitionsRequest listResourceDefinitionsRequest = new ListResourceDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listResourceDefinitionsRequest).stream();
    }

    @Override
    public Stream<DefinitionInformation> listConnectorDefinitions() {
        ListConnectorDefinitionsRequest listConnectorDefinitionsRequest = new ListConnectorDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listConnectorDefinitionsRequest).stream();
    }

    @Override
    public void deleteResourceDefinition(DefinitionInformation definitionInformation) {
        DeleteResourceDefinitionRequest deleteResourceDefinitionRequest = new DeleteResourceDefinitionRequest()
                .withResourceDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteResourceDefinition(deleteResourceDefinitionRequest);
    }

    @Override
    public void deleteConnectorDefinition(DefinitionInformation definitionInformation) {
        DeleteConnectorDefinitionRequest deleteConnectorDefinitionRequest = new DeleteConnectorDefinitionRequest()
                .withConnectorDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteConnectorDefinition(deleteConnectorDefinitionRequest);
    }

    @Override
    public String getResourceDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getResourceDefinitionVersionArn();
    }

    @Override
    public String getSubscriptionDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getSubscriptionDefinitionVersionArn();
    }

    @Override
    public GetSubscriptionDefinitionResult getSubscriptionDefinition(String groupId, VersionInformation versionInformation) {
        String subscriptionDefinitionVersionArn = getSubscriptionDefinitionVersionArn(groupId, versionInformation);

        GetSubscriptionDefinitionRequest getSubscriptionDefinitionRequest = new GetSubscriptionDefinitionRequest()
                .withSubscriptionDefinitionId(greengrassIdExtractor.extractId(subscriptionDefinitionVersionArn));
        return awsGreengrassClient.getSubscriptionDefinition(getSubscriptionDefinitionRequest);
    }

    @Override
    public GetSubscriptionDefinitionVersionResult getSubscriptionDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String subscriptionDefinitionVersionArn = getSubscriptionDefinitionVersionArn(groupId, versionInformation);

        if (subscriptionDefinitionVersionArn == null) {
            return null;
        }

        GetSubscriptionDefinitionVersionRequest getSubscriptionDefinitionVersionRequest = new GetSubscriptionDefinitionVersionRequest()
                .withSubscriptionDefinitionId(greengrassIdExtractor.extractId(subscriptionDefinitionVersionArn))
                .withSubscriptionDefinitionVersionId(greengrassIdExtractor.extractVersionId(subscriptionDefinitionVersionArn));
        return awsGreengrassClient.getSubscriptionDefinitionVersion(getSubscriptionDefinitionVersionRequest);
    }

    @Override
    public boolean deleteGroup(String groupId) {
        if (isGroupImmutable(groupId)) {
            // Don't delete a definition for an immutable group
            log.info("Skipping group [" + groupId + "] because it is an immutable group");
            return false;
        }

        try {
            ResetDeploymentsRequest resetDeploymentsRequest = new ResetDeploymentsRequest()
                    .withGroupId(groupId);

            awsGreengrassClient.resetDeployments(resetDeploymentsRequest);
        } catch (AWSGreengrassException e) {
            // Ignore
        }

        DeleteGroupRequest deleteGroupRequest = new DeleteGroupRequest()
                .withGroupId(groupId);
        awsGreengrassClient.deleteGroup(deleteGroupRequest);
        log.info("Deleted group [" + groupId + "]");

        return true;
    }

    private String getDeviceDefinitionVersionArn(String groupId, VersionInformation versionInformation) {
        GetGroupVersionResult getGroupVersionResult = getGroupVersion(groupId, versionInformation);
        GroupVersion groupVersion = getGroupVersionResult.getDefinition();

        return groupVersion.getDeviceDefinitionVersionArn();
    }

    @Override
    public GetDeviceDefinitionVersionResult getDeviceDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String deviceDefinitionVersionArn = getDeviceDefinitionVersionArn(groupId, versionInformation);

        if (deviceDefinitionVersionArn == null) {
            return null;
        }

        GetDeviceDefinitionVersionRequest getDeviceDefinitionVersionRequest = new GetDeviceDefinitionVersionRequest()
                .withDeviceDefinitionId(greengrassIdExtractor.extractId(deviceDefinitionVersionArn))
                .withDeviceDefinitionVersionId(greengrassIdExtractor.extractVersionId(deviceDefinitionVersionArn));
        return awsGreengrassClient.getDeviceDefinitionVersion(getDeviceDefinitionVersionRequest);
    }

    @Override
    public GetConnectorDefinitionVersionResult getConnectorDefinitionVersion(String groupId, VersionInformation versionInformation) {
        String connectorDefinitionVersionArn = getConnectorDefinitionVersionArn(groupId, versionInformation);

        if (connectorDefinitionVersionArn == null) {
            return null;
        }

        return getConnectorDefinitionVersionResult(greengrassIdExtractor.extractId(connectorDefinitionVersionArn), greengrassIdExtractor.extractVersionId(connectorDefinitionVersionArn));
    }

    private GetConnectorDefinitionVersionResult getConnectorDefinitionVersionResult(String connectorDefinitionId, String connectorDefinitionVersionId) {
        GetConnectorDefinitionVersionRequest getConnectorDefinitionVersionRequest = new GetConnectorDefinitionVersionRequest()
                .withConnectorDefinitionId(connectorDefinitionId)
                .withConnectorDefinitionVersionId(connectorDefinitionVersionId);
        return awsGreengrassClient.getConnectorDefinitionVersion(getConnectorDefinitionVersionRequest);
    }

    @Override
    public Stream<DefinitionInformation> listCoreDefinitions() {
        ListCoreDefinitionsRequest listCoreDefinitionsRequest = new ListCoreDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listCoreDefinitionsRequest).stream();
    }

    @Override
    public void deleteCoreDefinition(DefinitionInformation definitionInformation) {
        DeleteCoreDefinitionRequest deleteCoreDefinitionRequest = new DeleteCoreDefinitionRequest()
                .withCoreDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteCoreDefinition(deleteCoreDefinitionRequest);
    }

    private boolean isCoreDefinitionImmutable(DefinitionInformation definitionInformation) {
        GetCoreDefinitionVersionResult coreDefinitionVersionResult = getCoreDefinitionVersion(definitionInformation.getId(), definitionInformation.getLatestVersion());

        String coreThingArn = coreDefinitionVersionResult.getDefinition().getCores().get(0).getThingArn();
        return thingHelperProvider.get().isThingArnImmutable(coreThingArn);
    }

    @Override
    public Stream<DefinitionInformation> listFunctionDefinitions() {
        ListFunctionDefinitionsRequest listFunctionDefinitionsRequest = new ListFunctionDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listFunctionDefinitionsRequest).stream();
    }

    @Override
    public void deleteFunctionDefinition(DefinitionInformation definitionInformation) {
        DeleteFunctionDefinitionRequest deleteFunctionDefinitionRequest = new DeleteFunctionDefinitionRequest()
                .withFunctionDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteFunctionDefinition(deleteFunctionDefinitionRequest);
    }

    @Override
    public Stream<DefinitionInformation> listSubscriptionDefinitions() {
        ListSubscriptionDefinitionsRequest listSubscriptionDefinitionsRequest = new ListSubscriptionDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listSubscriptionDefinitionsRequest).stream();
    }

    @Override
    public void deleteSubscriptionDefinition(DefinitionInformation definitionInformation) {
        DeleteSubscriptionDefinitionRequest deleteSubscriptionDefinitionRequest = new DeleteSubscriptionDefinitionRequest()
                .withSubscriptionDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteSubscriptionDefinition(deleteSubscriptionDefinitionRequest);
    }

    @Override
    public Stream<DefinitionInformation> listDeviceDefinitions() {
        ListDeviceDefinitionsRequest listDeviceDefinitionsRequest = new ListDeviceDefinitionsRequest();

        return new V1ResultsIterator<DefinitionInformation>(awsGreengrassClient, listDeviceDefinitionsRequest).stream();
    }

    @Override
    public void deleteDeviceDefinition(DefinitionInformation definitionInformation) {
        DeleteDeviceDefinitionRequest deleteDeviceDefinitionRequest = new DeleteDeviceDefinitionRequest()
                .withDeviceDefinitionId(definitionInformation.getId());

        awsGreengrassClient.deleteDeviceDefinition(deleteDeviceDefinitionRequest);
    }

    @Override
    public boolean groupExists(String groupId) {
        GetGroupRequest getGroupRequest = new GetGroupRequest()
                .withGroupId(groupId);

        try {
            awsGreengrassClient.getGroup(getGroupRequest);

            return true;
        } catch (AWSGreengrassException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }

            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public boolean isGroupImmutable(String groupId) {
        if (!groupExists(groupId)) {
            return false;
        }

        Optional<VersionInformation> optionalLatestGroupVersion = getLatestGroupVersion(groupId);

        if (!optionalLatestGroupVersion.isPresent()) {
            return false;
        }

        VersionInformation latestGroupVersion = optionalLatestGroupVersion.get();

        if (!coreDefinitionVersionExists(latestGroupVersion)) {
            return false;
        }

        GetCoreDefinitionVersionResult coreDefinitionVersionResult = getCoreDefinitionVersion(groupId, latestGroupVersion);
        String coreThingArn = coreDefinitionVersionResult.getDefinition().getCores().get(0).getThingArn();

        V1ThingHelper v1ThingHelper = thingHelperProvider.get();

        return v1ThingHelper.isThingArnImmutable(coreThingArn);
    }

    private boolean coreDefinitionVersionExists(VersionInformation latestGroupVersion) {
        try {
            getCoreDefinitionVersion(latestGroupVersion.getId(), latestGroupVersion);

            return true;
        } catch (AWSGreengrassException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }

            throw new UnsupportedOperationException(e);
        }
    }

    private Stream<Deployment> sortDeployments(Stream<Deployment> deploymentsStream) {
        return deploymentsStream.sorted(Comparator.comparing(Deployment::getCreatedAt));
    }

    private Stream<VersionInformation> sortGroupVersionInformation(Stream<VersionInformation> versionInformationStream) {
        return versionInformationStream.sorted(Comparator.comparing(VersionInformation::getCreationTimestamp));
    }

    private Stream<GroupInformation> sortGroupInformation(Stream<GroupInformation> groupInformationStream) {
        return groupInformationStream.sorted(Comparator.comparing(GroupInformation::getName));
    }
}

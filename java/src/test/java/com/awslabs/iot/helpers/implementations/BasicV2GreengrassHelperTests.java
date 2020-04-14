package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.data.GreengrassGroupId;
import com.awslabs.iot.data.ImmutableGreengrassGroupId;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.Deployment;
import software.amazon.awssdk.services.greengrass.model.GroupInformation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.TestHelper.testNotMeaningfulWithoutAtLeast;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasicV2GreengrassHelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicV2GreengrassHelperTests.class);
    private GreengrassClient greengrassClient;
    private JsonHelper jsonHelper;
    private V2GreengrassHelper v2GreengrassHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        v2GreengrassHelper = injector.v2GreengrassHelper();
        greengrassClient = injector.greengrassClient();
        jsonHelper = injector.jsonHelper();
    }

    @Test
    public void shouldListGreengrassGroupsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        logStreamData(getStream);
    }

    private <T> void logStreamData(Callable<Stream<T>> getStream) throws Exception {
        getStream.call().forEach(object -> log.info(jsonHelper.toJson(object)));
    }

    @Test
    public void shouldListGreengrassGroupSubscriptionsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        List<GroupInformation> groupInformationList = getStream.call().collect(Collectors.toList());

        Long numberOfSubscriptions = groupInformationList.stream()
                .map(groupInformation -> v2GreengrassHelper.getSubscriptions(groupInformation))
                .map(TestHelper::logAndCount)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("subscriptions", numberOfSubscriptions);
    }

    @Test
    public void shouldListGreengrassDeploymentsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        Long numberOfDeployments = getGroupInformationStream.call()
                .map(groupInformation -> v2GreengrassHelper.getDeployments(groupInformation))
                .map(TestHelper::logAndCount)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("Greengrass deployments", numberOfDeployments);
    }

    @Test
    public void shouldReturnEmptyDeploymentStatusWithInvalidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        GreengrassGroupId groupId = getGroupInformationStream.call()
                .findFirst()
                .map(GroupInformation::id)
                .map(id -> ImmutableGreengrassGroupId.builder().groupId(id).build())
                .get();

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass deployments", getDeploymentsStream.call());

        Deployment deployment = v2GreengrassHelper.getDeployments(groupId)
                .findFirst()
                .get();

        deployment = deployment.toBuilder().deploymentId(deployment.deploymentId() + "1").build();

        assertThat(v2GreengrassHelper.getDeploymentStatusResponse(groupId, deployment), is(Optional.empty()));
    }

    @Test
    public void shouldNotReturnEmptyDeploymentStatusWithValidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        GreengrassGroupId groupId = getGroupInformationStream.call()
                .findFirst()
                .map(GroupInformation::id)
                .map(id -> ImmutableGreengrassGroupId.builder().groupId(id).build())
                .get();

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass deployments", getDeploymentsStream.call());

        Deployment deployment = getDeploymentsStream.call()
                .findFirst()
                .get();

        assertThat(v2GreengrassHelper.getDeploymentStatusResponse(groupId, deployment), is(not(Optional.empty())));
    }

    @Test
    public void shouldReturnLatestDeployment() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        GreengrassGroupId groupId = getGroupInformationStream.call()
                .findFirst()
                .map(GroupInformation::id)
                .map(id -> ImmutableGreengrassGroupId.builder().groupId(id).build())
                .get();

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithoutAtLeast("Greengrass deployments", getDeploymentsStream.call(), 2);

        Long latestDeploymentCreatedAt = v2GreengrassHelper.getLatestDeployment(groupId)
                .map(Deployment::createdAt)
                .map(Instant::parse)
                .map(Instant::toEpochMilli)
                .get();

        Long maxCreatedAt = v2GreengrassHelper.getDeployments(groupId)
                .map(Deployment::createdAt)
                .map(Instant::parse)
                .map(Instant::toEpochMilli)
                .max(Long::compareTo)
                .get();

        assertThat(latestDeploymentCreatedAt, is(maxCreatedAt));
    }
}

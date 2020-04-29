package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iam.data.ImmutableRoleName;
import com.awslabs.iam.data.RoleName;
import com.awslabs.iot.data.GreengrassGroupId;
import com.awslabs.iot.data.ImmutableGreengrassGroupId;
import com.awslabs.iot.data.ImmutableThingName;
import com.awslabs.iot.data.ThingName;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import io.vavr.Tuple2;
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

        Optional<ImmutableGreengrassGroupId> optionalGroupId = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 1);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = optionalGroupId.orElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass deployments", getDeploymentsStream.call());

        Deployment deployment = v2GreengrassHelper.getDeployments(groupId)
                .findFirst()
                .get();

        deployment = deployment.toBuilder().deploymentId(String.join("", deployment.deploymentId(), "1")).build();

        assertThat(v2GreengrassHelper.getDeploymentStatusResponse(groupId, deployment), is(Optional.empty()));
    }

    @Test
    public void shouldNotReturnEmptyDeploymentStatusWithValidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        Optional<ImmutableGreengrassGroupId> optionalGroupId = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 1);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = optionalGroupId.orElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass groups with deployments", getDeploymentsStream.call());

        Deployment deployment = getDeploymentsStream.call()
                .findFirst()
                .get();

        assertThat(v2GreengrassHelper.getDeploymentStatusResponse(groupId, deployment), is(not(Optional.empty())));
    }

    @Test
    public void shouldReturnLatestDeployment() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        // Must have at least two deployments in a single group so we can make sure the latest deployment is returned properly
        Optional<ImmutableGreengrassGroupId> optionalGroupId = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 2);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = optionalGroupId.orElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithoutAtLeast("Greengrass deployments in any group", getDeploymentsStream.call(), 2);

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

    private Optional<ImmutableGreengrassGroupId> getGroupIdWithXorMoreDeployments(Callable<Stream<GroupInformation>> getGroupInformationStream, int minimumNumberOfDeployments) throws Exception {
        return getGroupInformationStream.call()
                    .map(GroupInformation::id)
                    .map(id -> ImmutableGreengrassGroupId.builder().groupId(id).build())
                    // Get the deployments for each group and create a tuple with the group ID and stream of deployments
                    .map(id -> new Tuple2<>(id, v2GreengrassHelper.getDeployments(id)))
                    // Make sure we find a group with a minimum number of deployments
                    .filter(tuple -> tuple._2.count() >= minimumNumberOfDeployments)
                    .findAny()
                    .map(tuple -> tuple._1);
    }

    @Test
    public void shouldCreateARaspbianSoftwareUpdateJobAndNotThrowAnException() {
        String thingNameString = "pi_Core";
        ThingName thingName = ImmutableThingName.builder().name(thingNameString).build();
        String roleNameString = "IotS3UrlPresigningRole";
        RoleName roleName = ImmutableRoleName.builder().name(roleNameString).build();
        v2GreengrassHelper.updateRaspbianCore(thingName, roleName);
    }
}

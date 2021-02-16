package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iam.data.ImmutableRoleName;
import com.awslabs.iam.data.RoleName;
import com.awslabs.iam.helpers.interfaces.V2IamHelper;
import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.Deployment;
import software.amazon.awssdk.services.greengrass.model.GroupInformation;
import software.amazon.awssdk.services.iot.model.ThingAttribute;

import java.time.Instant;
import java.util.concurrent.Callable;

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
    private V2IamHelper v2IamHelper;
    private V2IotHelper v2IotHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        v2GreengrassHelper = injector.v2GreengrassHelper();
        v2IamHelper = injector.v2IamHelper();
        v2IotHelper = injector.v2IotHelper();
        greengrassClient = injector.greengrassClient();
        jsonHelper = injector.jsonHelper();
    }

    @Test
    public void shouldListGreengrassGroupsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> Stream.ofAll(v2GreengrassHelper.getGroups());
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        logStreamData(getStream);
    }

    private <T> void logStreamData(Callable<Stream<T>> getStream) throws Exception {
        getStream.call().forEach(object -> log.info(jsonHelper.toJson(object)));
    }

    @Test
    public void shouldListGreengrassGroupSubscriptionsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> Stream.ofAll(v2GreengrassHelper.getGroups());
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        int numberOfSubscriptions = TestHelper.logAndCount(getStream.call()
                .flatMap(groupInformation -> v2GreengrassHelper.getSubscriptions(groupInformation))
                // Flatten the list of lists
                .flatMap(Stream::ofAll));

        testNotMeaningfulWithout("subscriptions", numberOfSubscriptions);
    }

    @Test
    public void shouldListGreengrassDeploymentsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        int numberOfDeployments = TestHelper.logAndCount(getGroupInformationStream.call()
                .flatMap(groupInformation -> v2GreengrassHelper.getDeployments(groupInformation)));

        testNotMeaningfulWithout("Greengrass deployments", numberOfDeployments);
    }

    @Test
    public void shouldReturnEmptyDeploymentStatusWithInvalidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        Option<ImmutableGreengrassGroupId> groupIdOption = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 1);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = groupIdOption.getOrElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass deployments", getDeploymentsStream.call());

        Deployment deployment = v2GreengrassHelper.getDeployments(groupId).get();

        deployment = deployment.toBuilder().deploymentId(String.join("", deployment.deploymentId(), "1")).build();

        assertThat(v2GreengrassHelper.getDeploymentStatusResponse(groupId, deployment), is(Option.none()));
    }

    @Test
    public void shouldNotReturnEmptyDeploymentStatusWithValidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        Option<ImmutableGreengrassGroupId> groupIdOption = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 1);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = groupIdOption.getOrElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> v2GreengrassHelper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass groups with deployments", getDeploymentsStream.call());

        Deployment deployment = getDeploymentsStream.call().get();

        assertThat(v2GreengrassHelper.getDeploymentStatusResponse(groupId, deployment), is(not(Option.none())));
    }

    @Test
    public void shouldReturnLatestDeployment() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        // Must have at least two deployments in a single group so we can make sure the latest deployment is returned properly
        Option<ImmutableGreengrassGroupId> groupIdOption = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 2);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = groupIdOption.getOrElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

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
                .sorted()
                .reverse()
                .get();

        assertThat(latestDeploymentCreatedAt, is(maxCreatedAt));
    }

    private Option<ImmutableGreengrassGroupId> getGroupIdWithXorMoreDeployments(Callable<Stream<GroupInformation>> getGroupInformationStream, int minimumNumberOfDeployments) throws Exception {
        return Option.of(getGroupInformationStream.call()
                .map(GroupInformation::id)
                .map(id -> ImmutableGreengrassGroupId.builder().groupId(id).build())
                // Get the deployments for each group and create a tuple with the group ID and stream of deployments
                .map(id -> new Tuple2<>(id, v2GreengrassHelper.getDeployments(id)))
                // Make sure we find a group with a minimum number of deployments
                .filter(tuple -> tuple._2.size() >= minimumNumberOfDeployments)
                .getOrNull())
                .map(tuple -> tuple._1);
    }

    @Test
    public void shouldCreateARaspbianSoftwareUpdateJobAndNotThrowAnException() {
        String groupNameString = "pi";
        GreengrassGroupName greengrassGroupName = ImmutableGreengrassGroupName.builder().groupName(groupNameString).build();
        testNotMeaningfulWithout("pi_Core Greengrass group", v2GreengrassHelper.getGroupInformation(greengrassGroupName));

        String coreNameString = String.join("_", groupNameString, "Core");
        testNotMeaningfulWithout("pi_Core thing", v2IotHelper.getThings().filter(thingAttribute -> thingAttribute.thingName().equals(coreNameString)));

        String roleNameString = "IotS3UrlPresigningRole";
        RoleName roleName = ImmutableRoleName.builder().name(roleNameString).build();
        testNotMeaningfulWithout("IotS3UrlPresigning role in IAM", v2IamHelper.getRole(roleName).toStream());

        ThingName thingName = ImmutableThingName.builder().name(coreNameString).build();
        v2GreengrassHelper.updateRaspbianCore(thingName, roleName);
    }
}

package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.iam.data.ImmutableRoleName;
import com.awslabs.iam.data.RoleName;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.GreengrassV1Helper;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.resultsiterator.implementations.DaggerTestInjector;
import com.awslabs.resultsiterator.implementations.TestInjector;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.model.Deployment;
import software.amazon.awssdk.services.greengrass.model.GroupInformation;

import java.time.Instant;
import java.util.concurrent.Callable;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.TestHelper.testNotMeaningfulWithoutAtLeast;
import static com.awslabs.general.helpers.implementations.JsonHelper.toJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasicGreengrassV1HelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicGreengrassV1HelperTests.class);
    private GreengrassV1Helper greengrassV1Helper;
    private IamHelper iamHelper;
    private IotHelper iotHelper;

    @Before
    public void setup() {
        TestInjector injector = DaggerTestInjector.create();
        greengrassV1Helper = injector.greengrassHelper();
        iamHelper = injector.iamHelper();
        iotHelper = injector.iotHelper();
    }

    @Test
    public void shouldListGreengrassGroupsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> greengrassV1Helper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        logStreamData(getStream);
    }

    private <T> void logStreamData(Callable<Stream<T>> getStream) throws Exception {
        getStream.call().forEach(object -> log.info(toJson(object)));
    }

    @Test
    public void shouldListGreengrassGroupSubscriptionsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> greengrassV1Helper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        int numberOfSubscriptions = TestHelper.logAndCount(getStream.call()
                .flatMap(groupInformation -> greengrassV1Helper.getSubscriptions(groupInformation))
                // Flatten the list of lists
                .flatMap(Stream::ofAll));

        testNotMeaningfulWithout("subscriptions", numberOfSubscriptions);
    }

    @Test
    public void shouldListGreengrassDeploymentsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> greengrassV1Helper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        int numberOfDeployments = TestHelper.logAndCount(getGroupInformationStream.call()
                .flatMap(groupInformation -> greengrassV1Helper.getDeployments(groupInformation)));

        testNotMeaningfulWithout("Greengrass deployments", numberOfDeployments);
    }

    @Test
    public void shouldReturnEmptyDeploymentStatusWithInvalidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> greengrassV1Helper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        Option<ImmutableGreengrassGroupId> groupIdOption = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 1);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = groupIdOption.getOrElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> greengrassV1Helper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass deployments", getDeploymentsStream.call());

        Deployment deployment = greengrassV1Helper.getDeployments(groupId).get();

        deployment = deployment.toBuilder().deploymentId(String.join("", deployment.deploymentId(), "1")).build();

        assertThat(greengrassV1Helper.getDeploymentStatusResponse(groupId, deployment), is(Option.none()));
    }

    @Test
    public void shouldNotReturnEmptyDeploymentStatusWithValidId() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> greengrassV1Helper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        Option<ImmutableGreengrassGroupId> groupIdOption = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 1);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = groupIdOption.getOrElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> greengrassV1Helper.getDeployments(groupId);
        testNotMeaningfulWithout("Greengrass groups with deployments", getDeploymentsStream.call());

        Deployment deployment = getDeploymentsStream.call().get();

        assertThat(greengrassV1Helper.getDeploymentStatusResponse(groupId, deployment), is(not(Option.none())));
    }

    @Test
    public void shouldReturnLatestDeployment() throws Exception {
        Callable<Stream<GroupInformation>> getGroupInformationStream = () -> greengrassV1Helper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getGroupInformationStream.call());

        // Must have at least two deployments in a single group so we can make sure the latest deployment is returned properly
        Option<ImmutableGreengrassGroupId> groupIdOption = getGroupIdWithXorMoreDeployments(getGroupInformationStream, 2);

        // Use the group ID we found or a fake value if we didn't find any
        ImmutableGreengrassGroupId groupId = groupIdOption.getOrElse(ImmutableGreengrassGroupId.builder().groupId("fake").build());

        Callable<Stream<Deployment>> getDeploymentsStream = () -> greengrassV1Helper.getDeployments(groupId);
        testNotMeaningfulWithoutAtLeast("Greengrass deployments in any group", getDeploymentsStream.call(), 2);

        Long latestDeploymentCreatedAt = greengrassV1Helper.getLatestDeployment(groupId)
                .map(Deployment::createdAt)
                .map(Instant::parse)
                .map(Instant::toEpochMilli)
                .get();

        Long maxCreatedAt = greengrassV1Helper.getDeployments(groupId)
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
                .map(id -> new Tuple2<>(id, greengrassV1Helper.getDeployments(id)))
                // Make sure we find a group with a minimum number of deployments
                .filter(tuple -> tuple._2.size() >= minimumNumberOfDeployments)
                .getOrNull())
                .map(tuple -> tuple._1);
    }

    @Test
    public void shouldCreateARaspbianSoftwareUpdateJobAndNotThrowAnException() {
        String groupNameString = "pi";
        GreengrassGroupName greengrassGroupName = ImmutableGreengrassGroupName.builder().groupName(groupNameString).build();
        testNotMeaningfulWithout("pi_Core Greengrass group", greengrassV1Helper.getGroupInformation(greengrassGroupName));

        String coreNameString = String.join("_", groupNameString, "Core");
        testNotMeaningfulWithout("pi_Core thing", iotHelper.getThings().filter(thingAttribute -> thingAttribute.thingName().equals(coreNameString)));

        String roleNameString = "IotS3UrlPresigningRole";
        RoleName roleName = ImmutableRoleName.builder().name(roleNameString).build();
        testNotMeaningfulWithout("IotS3UrlPresigning role in IAM", iamHelper.getRole(roleName).toStream());

        ThingName thingName = ImmutableThingName.builder().name(coreNameString).build();
        greengrassV1Helper.updateRaspbianCore(thingName, roleName);
    }
}

package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.data.ImmutableCertificateArn;
import com.awslabs.iot.data.ImmutableThingName;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.iot.helpers.implementations.BasicV2IotHelper.DELIMITER;
import static com.awslabs.iot.helpers.implementations.BasicV2IotHelper.THING_GROUP_NAMES;
import static com.awslabs.resultsiterator.TestV2ResultsIterator.JUNKFORGROUPTESTING_V2;
import static com.awslabs.resultsiterator.TestV2ResultsIterator.JUNKFORTHINGTESTING_V2;

public class BasicV2IotHelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicV2IotHelperTests.class);
    private IotClient iotClient;
    private JsonHelper jsonHelper;
    private V2IotHelper v2IotHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        v2IotHelper = injector.v2IotHelper();
        iotClient = injector.iotClient();
        jsonHelper = injector.jsonHelper();

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING_V2)
                .build();
        iotClient.createThing(createThingRequest);

        CreateThingGroupRequest createThingGroupRequest = CreateThingGroupRequest.builder()
                .thingGroupName(JUNKFORGROUPTESTING_V2)
                .build();
        iotClient.createThingGroup(createThingGroupRequest);

        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING_V2)
                .build();

        DescribeThingResponse describeThingResponse = iotClient.describeThing(describeThingRequest);

        AddThingToThingGroupRequest addThingToThingGroupRequest = AddThingToThingGroupRequest.builder()
                .thingArn(describeThingResponse.thingArn())
                .thingGroupName(JUNKFORGROUPTESTING_V2)
                .build();
        iotClient.addThingToThingGroup(addThingToThingGroupRequest);
    }

    @After
    public void tearDown() {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING_V2)
                .build();
        iotClient.deleteThing(deleteThingRequest);

        DeleteThingGroupRequest deleteThingGroupRequest = DeleteThingGroupRequest.builder()
                .thingGroupName(JUNKFORGROUPTESTING_V2)
                .build();
        iotClient.deleteThingGroup(deleteThingGroupRequest);
    }

    @Test
    public void shouldListAttachedThingsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<Certificate>> getCertificatesStream = () -> v2IotHelper.getCertificates();
        testNotMeaningfulWithout("certificates", getCertificatesStream.call());

        Long numberOfAttachedThings = getCertificatesStream.call()
                .map(certificate -> ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build())
                .map(v2IotHelper::getAttachedThings)
                .map(TestHelper::logAndCount)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("things attached to certificates", numberOfAttachedThings);
    }

    @Test
    public void shouldListAttachedPoliciesWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<Certificate>> getCertificatesStream = () -> v2IotHelper.getCertificates();
        testNotMeaningfulWithout("certificates", getCertificatesStream.call());

        Long numberOfAttachedThings = getCertificatesStream.call()
                .map(certificate -> ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build())
                .map(v2IotHelper::getAttachedPolicies)
                .map(TestHelper::logAndCount)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("policies attached to certificates", numberOfAttachedThings);
    }

    @Test
    public void shouldListThingPrincipalsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<ThingAttribute>> getThingsStream = () -> v2IotHelper.getThings();
        testNotMeaningfulWithout("things", getThingsStream.call());

        Long numberOfThingPrincipals = getThingsStream.call()
                .map(thingAttribute -> ImmutableThingName.builder().name(thingAttribute.thingName()).build())
                .map(v2IotHelper::getThingPrincipals)
                .map(TestHelper::logAndCount)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("principals attached to things", numberOfThingPrincipals);
    }

    @Test
    public void shouldListJobsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<JobSummary>> getJobsStream = () -> v2IotHelper.getJobs();
        testNotMeaningfulWithout("jobs", getJobsStream.call());
    }

    @Test
    public void shouldListJobExecutionsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<JobSummary>> getJobsStream = () -> v2IotHelper.getJobs();
        testNotMeaningfulWithout("jobs", getJobsStream.call());

        JobSummary jobSummary = getJobsStream.call().findFirst().get();
        Callable<Stream<JobExecutionSummaryForJob>> getJobsExecutionsStream = () -> v2IotHelper.getJobExecutions(jobSummary);

        testNotMeaningfulWithout("job executions", getJobsExecutionsStream.call());
    }


    private void waitForNonZeroFleetIndexResult(Callable<Stream> streamCallable) {
        // Wait for the fleet index to settle
        RetryPolicy<Long> fleetIndexRetryPolicy = new RetryPolicy<Long>()
                .handleResult(0L)
                .withDelay(Duration.ofSeconds(5))
                .withMaxRetries(10)
                .onRetry(failure -> log.warn("Waiting for non-zero fleet index result..."))
                .onRetriesExceeded(failure -> log.error("Fleet index never returned results, giving up"));

        Failsafe.with(fleetIndexRetryPolicy).get(() -> streamCallable.call().count());
    }

    @Test
    public void shouldGetThingDocumentsInsteadOfThingGroups() throws Exception {
        Callable<Stream> streamCallable = () -> v2IotHelper.getThingsByGroupName(JUNKFORGROUPTESTING_V2);
        waitForNonZeroFleetIndexResult(streamCallable);

        testNotMeaningfulWithout("things in thing groups", streamCallable.call());
    }

    @Test
    public void shouldThrowExceptionDueToTypeErasureAmbiguityWhenRequestingSearchIndexResults() {
        String queryString = String.join(DELIMITER, THING_GROUP_NAMES, "*");

        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString(queryString)
                .build();

        UnsupportedOperationException unsupportedOperationException = Assert.assertThrows(UnsupportedOperationException.class, () -> new V2ResultsIterator<ThingDocument>(iotClient, searchIndexRequest).stream().count());
        Assert.assertTrue(unsupportedOperationException.getMessage().contains("Multiple methods found"));
    }
}

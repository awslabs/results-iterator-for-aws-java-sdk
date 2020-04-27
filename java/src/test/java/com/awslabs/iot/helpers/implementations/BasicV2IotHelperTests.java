package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.data.ImmutableCertificateArn;
import com.awslabs.iot.data.ImmutableThingName;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.Certificate;
import software.amazon.awssdk.services.iot.model.JobExecutionSummaryForJob;
import software.amazon.awssdk.services.iot.model.JobSummary;
import software.amazon.awssdk.services.iot.model.ThingAttribute;

import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;

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
}

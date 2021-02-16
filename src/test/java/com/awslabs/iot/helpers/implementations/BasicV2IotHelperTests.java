package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iam.helpers.interfaces.V2IamHelper;
import com.awslabs.iot.data.ImmutableCertificateArn;
import com.awslabs.iot.data.ImmutableThingName;
import com.awslabs.iot.data.V2IotEndpointType;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.concurrent.Callable;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.iot.helpers.implementations.BasicV2IotHelper.FLEET_INDEXING_QUERY_STRING_DELIMITER;
import static com.awslabs.iot.helpers.implementations.BasicV2IotHelper.THING_GROUP_NAMES;
import static com.awslabs.iot.helpers.interfaces.V2IotHelper.CN;
import static com.awslabs.iot.helpers.interfaces.V2IotHelper.SUBJECT_KEY_VALUE_SEPARATOR;
import static com.awslabs.resultsiterator.TestV2ResultsIterator.JUNKFORGROUPTESTING_V2;
import static com.awslabs.resultsiterator.TestV2ResultsIterator.JUNKFORTHINGTESTING_V2;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class BasicV2IotHelperTests {
    public static final String CSR_TEST_SUBJECT = "test1";
    public static final String CERTIFICATE_TEST_ORG = "partner-name";
    public static final String COMMON_NAME_PREFIX = CN + "=";
    private final Logger log = LoggerFactory.getLogger(BasicV2IotHelperTests.class);
    private IotClient iotClient;
    private JsonHelper jsonHelper;
    private V2IotHelper v2IotHelper;
    private V2IamHelper v2IamHelper;
    private List<Tuple2<String, String>> issuerName;
    private List<Tuple2<String, String>> subjectNameBeforeSigning;
    private List<Tuple2<String, String>> subjectNameAfterSigning;
    private String expectedCsrSubject;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        v2IotHelper = injector.v2IotHelper();
        iotClient = injector.iotClient();
        v2IamHelper = injector.v2IamHelper();
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

        issuerName = List.of(Tuple.of(CN, v2IamHelper.getAccountId().getId()));
        subjectNameBeforeSigning = List.of(Tuple.of(CN, CSR_TEST_SUBJECT));
        subjectNameAfterSigning = List.of(
                Tuple.of(CN, v2IotHelper.getEndpoint(V2IotEndpointType.DATA_ATS)),
                Tuple.of(V2IotHelper.O, CERTIFICATE_TEST_ORG));

        expectedCsrSubject = String.join(SUBJECT_KEY_VALUE_SEPARATOR, CN, CSR_TEST_SUBJECT);
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

        int numberOfAttachedThings = TestHelper.logAndCount(getCertificatesStream.call()
                .map(certificate -> ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build())
                .flatMap(v2IotHelper::getAttachedThings));

        testNotMeaningfulWithout("things attached to certificates", numberOfAttachedThings);
    }

    @Test
    public void shouldListAttachedPoliciesWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<Certificate>> getCertificatesStream = () -> v2IotHelper.getCertificates();
        testNotMeaningfulWithout("certificates", getCertificatesStream.call());

        int numberOfAttachedThings = TestHelper.logAndCount(getCertificatesStream.call()
                .map(certificate -> ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build())
                .flatMap(v2IotHelper::getAttachedPolicies));

        testNotMeaningfulWithout("policies attached to certificates", numberOfAttachedThings);
    }

    @Test
    public void shouldListThingPrincipalsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<ThingAttribute>> getThingsStream = () -> v2IotHelper.getThings();
        testNotMeaningfulWithout("things", getThingsStream.call());

        int numberOfThingPrincipals = TestHelper.logAndCount(getThingsStream.call()
                .map(thingAttribute -> ImmutableThingName.builder().name(thingAttribute.thingName()).build())
                .flatMap(v2IotHelper::getThingPrincipals));

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

        JobSummary jobSummary = getJobsStream.call().get();
        Callable<Stream<JobExecutionSummaryForJob>> getJobsExecutionsStream = () -> v2IotHelper.getJobExecutions(jobSummary);

        testNotMeaningfulWithout("job executions", getJobsExecutionsStream.call());
    }


    private void waitForNonZeroFleetIndexResult(Callable<Stream> streamCallable) {
        // Wait for the fleet index to settle
        RetryPolicy<Integer> fleetIndexRetryPolicy = new RetryPolicy<Integer>()
                .handleResult(0)
                .withDelay(Duration.ofSeconds(5))
                .withMaxRetries(10)
                .onRetry(failure -> log.warn("Waiting for non-zero fleet index result..."))
                .onRetriesExceeded(failure -> log.error("Fleet index never returned results, giving up"));

        Failsafe.with(fleetIndexRetryPolicy).get(() -> streamCallable.call().size());
    }

    @Test
    public void shouldGetThingDocumentsInsteadOfThingGroups() throws Exception {
        Callable<Stream> streamCallable = () -> v2IotHelper.getThingsByGroupName(JUNKFORGROUPTESTING_V2);
        waitForNonZeroFleetIndexResult(streamCallable);

        testNotMeaningfulWithout("things in thing groups", streamCallable.call());
    }

    @Test
    public void shouldThrowExceptionDueToTypeErasureAmbiguityWhenRequestingSearchIndexResults() {
        String queryString = String.join(FLEET_INDEXING_QUERY_STRING_DELIMITER, THING_GROUP_NAMES, "*");

        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString(queryString)
                .build();

        UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> new V2ResultsIterator<ThingDocument>(iotClient, searchIndexRequest).stream().size());
        assertThat(unsupportedOperationException.getMessage(), org.hamcrest.CoreMatchers.containsString("Multiple methods found"));
    }

    @Test
    public void shouldGenerateRandomKeypair() {
        getRandomRsaKeyPair();
    }

    private java.security.KeyPair getRandomRsaKeyPair() {
        return new BasicV2IotHelper().getRandomRsaKeypair(4096);
    }

    @Test
    public void shouldGenerateCertificateSigningRequest() {
        v2IotHelper.generateCertificateSigningRequest(getRandomRsaKeyPair(), subjectNameBeforeSigning);
    }

    @Test
    public void shouldGenerateCsrWithExpectedSubject() {
        PKCS10CertificationRequest pkcs10CertificationRequest = v2IotHelper.generateCertificateSigningRequest(getRandomRsaKeyPair(), subjectNameBeforeSigning);

        assertThat(pkcs10CertificationRequest.getSubject().toString(), is(expectedCsrSubject));
    }

    @Test
    public void shouldGenerateCertificateSigningRequestAndExtractPublicKey() {
        java.security.KeyPair randomRsaKeyPair = getRandomRsaKeyPair();
        RSAPublicKey expectedRsaPublicKey = (RSAPublicKey) randomRsaKeyPair.getPublic();
        PKCS10CertificationRequest pkcs10CertificationRequest = v2IotHelper.generateCertificateSigningRequest(randomRsaKeyPair, subjectNameBeforeSigning);
        RSAPublicKey rsaPublicKeyFromCsrPem = v2IotHelper.getRsaPublicKeyFromCsrPem(v2IotHelper.toPem(pkcs10CertificationRequest));
        assertThat(expectedRsaPublicKey.getPublicExponent(), is(rsaPublicKeyFromCsrPem.getPublicExponent()));
        assertThat(expectedRsaPublicKey.getModulus(), is(rsaPublicKeyFromCsrPem.getModulus()));
    }

    @Test
    public void shouldGenerateCertificateWithDifferentSubject() {
        PKCS10CertificationRequest pkcs10CertificationRequest = v2IotHelper.generateCertificateSigningRequest(getRandomRsaKeyPair(), subjectNameBeforeSigning);

        RSAPublicKey rsaPublicKeyFromCsrPem = v2IotHelper.getRsaPublicKeyFromCsrPem(v2IotHelper.toPem(pkcs10CertificationRequest));
        X509Certificate x509Certificate = v2IotHelper.generateX509Certificate(rsaPublicKeyFromCsrPem, issuerName, subjectNameAfterSigning);

        String issuerPrincipalString = x509Certificate.getIssuerX500Principal().getName();
        String subjectPrincipalString = x509Certificate.getSubjectDN().getName();

        assertThat(issuerPrincipalString, containsString(COMMON_NAME_PREFIX));
        assertThat(subjectPrincipalString, containsString(COMMON_NAME_PREFIX));
        assertThat(subjectPrincipalString, containsString(String.join(SUBJECT_KEY_VALUE_SEPARATOR, V2IotHelper.O, CERTIFICATE_TEST_ORG)));
        assertThat(subjectPrincipalString, is(not(expectedCsrSubject)));
    }

    @Test
    public void shouldRegisterCsr() {
        // Make sure that the CSRs we generate are valid
        java.security.KeyPair randomRsaKeyPair = getRandomRsaKeyPair();
        PKCS10CertificationRequest pkcs10CertificationRequest = v2IotHelper.generateCertificateSigningRequest(randomRsaKeyPair, subjectNameBeforeSigning);

        CreateCertificateFromCsrRequest createCertificateFromCsrRequest = CreateCertificateFromCsrRequest.builder()
                .certificateSigningRequest(v2IotHelper.toPem(pkcs10CertificationRequest))
                // Do not set it as active so we can delete it more easily later
                .setAsActive(false)
                .build();

        CreateCertificateFromCsrResponse createCertificateFromCsrResponse = iotClient.createCertificateFromCsr(createCertificateFromCsrRequest);

        // Make sure the subject return from AWS IoT matches what we expect
        X509CertificateHolder x509CertificateHolder = v2IotHelper.tryGetObjectFromPem(createCertificateFromCsrResponse.certificatePem(), X509CertificateHolder.class).get();
        assertThat(x509CertificateHolder.getSubject().toString(), is(expectedCsrSubject));

        // Cleanup
        DeleteCertificateRequest deleteCertificateRequest = DeleteCertificateRequest.builder()
                .certificateId(createCertificateFromCsrResponse.certificateId())
                .build();

        iotClient.deleteCertificate(deleteCertificateRequest);
    }

    @Test
    public void shouldRegisterCertificateWithoutCa() {
        // Make sure that the certificates we generate without a registered CA are valid
        java.security.KeyPair randomRsaKeyPair = getRandomRsaKeyPair();
        PKCS10CertificationRequest pkcs10CertificationRequest = v2IotHelper.generateCertificateSigningRequest(randomRsaKeyPair, subjectNameBeforeSigning);

        RSAPublicKey rsaPublicKeyFromCsrPem = v2IotHelper.getRsaPublicKeyFromCsrPem(v2IotHelper.toPem(pkcs10CertificationRequest));
        X509Certificate x509Certificate = v2IotHelper.generateX509Certificate(rsaPublicKeyFromCsrPem, issuerName, subjectNameAfterSigning);

        RegisterCertificateWithoutCaRequest registerCertificateWithoutCaRequest = RegisterCertificateWithoutCaRequest.builder()
                .certificatePem(v2IotHelper.toPem(x509Certificate))
                .build();

        RegisterCertificateWithoutCaResponse registerCertificateWithoutCaResponse = iotClient.registerCertificateWithoutCA(registerCertificateWithoutCaRequest);

        // Cleanup
        DeleteCertificateRequest deleteCertificateRequest = DeleteCertificateRequest.builder()
                .certificateId(registerCertificateWithoutCaResponse.certificateId())
                .build();

        iotClient.deleteCertificate(deleteCertificateRequest);
    }
}

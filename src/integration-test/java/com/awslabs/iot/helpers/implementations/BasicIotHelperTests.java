package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.awslabs.iot.data.ImmutableCertificateArn;
import com.awslabs.iot.data.ImmutableThingName;
import com.awslabs.iot.data.IotEndpointType;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.resultsiterator.implementations.DaggerTestInjector;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.awslabs.resultsiterator.implementations.TestInjector;
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
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.concurrent.Callable;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.iot.helpers.implementations.BasicIotHelper.*;
import static com.awslabs.iot.helpers.interfaces.IotHelper.CN;
import static com.awslabs.iot.helpers.interfaces.IotHelper.SUBJECT_KEY_VALUE_SEPARATOR;
import static com.awslabs.resultsiterator.TestResultsIterator.JUNKFORGROUPTESTING;
import static com.awslabs.resultsiterator.TestResultsIterator.JUNKFORTHINGTESTING;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class BasicIotHelperTests {
    public static final String CSR_TEST_SUBJECT = "thing";
    public static final String CERTIFICATE_TEST_ORG = "partner-name";
    public static final String COMMON_NAME_PREFIX = CN + "=";
    private final Logger log = LoggerFactory.getLogger(BasicIotHelperTests.class);
    private IotClient iotClient;
    private IotHelper iotHelper;
    private IamHelper iamHelper;
    private List<Tuple2<String, String>> issuerName;
    private List<Tuple2<String, String>> subjectNameBeforeSigning;
    private List<Tuple2<String, String>> subjectNameAfterSigning;
    private String expectedCsrSubject;

    @Before
    public void setup() {
        TestInjector injector = DaggerTestInjector.create();
        iotHelper = injector.iotHelper();
        iotClient = injector.iotClient();
        iamHelper = injector.iamHelper();

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING)
                .build();
        iotClient.createThing(createThingRequest);

        CreateThingGroupRequest createThingGroupRequest = CreateThingGroupRequest.builder()
                .thingGroupName(JUNKFORGROUPTESTING)
                .build();
        iotClient.createThingGroup(createThingGroupRequest);

        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING)
                .build();

        DescribeThingResponse describeThingResponse = iotClient.describeThing(describeThingRequest);

        AddThingToThingGroupRequest addThingToThingGroupRequest = AddThingToThingGroupRequest.builder()
                .thingArn(describeThingResponse.thingArn())
                .thingGroupName(JUNKFORGROUPTESTING)
                .build();
        iotClient.addThingToThingGroup(addThingToThingGroupRequest);

        issuerName = List.of(Tuple.of(CN, iamHelper.getAccountId().getId()));
        subjectNameBeforeSigning = List.of(Tuple.of(CN, CSR_TEST_SUBJECT));
        subjectNameAfterSigning = List.of(
                Tuple.of(CN, iotHelper.getEndpoint(IotEndpointType.DATA_ATS)),
                Tuple.of(IotHelper.O, CERTIFICATE_TEST_ORG));

        expectedCsrSubject = String.join(SUBJECT_KEY_VALUE_SEPARATOR, CN, CSR_TEST_SUBJECT);
    }

    @After
    public void tearDown() {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING)
                .build();
        iotClient.deleteThing(deleteThingRequest);

        DeleteThingGroupRequest deleteThingGroupRequest = DeleteThingGroupRequest.builder()
                .thingGroupName(JUNKFORGROUPTESTING)
                .build();
        iotClient.deleteThingGroup(deleteThingGroupRequest);
    }

    @Test
    public void shouldListAttachedThingsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<Certificate>> getCertificatesStream = () -> iotHelper.getCertificates();
        testNotMeaningfulWithout("certificates", getCertificatesStream.call());

        int numberOfAttachedThings = TestHelper.logAndCount(getCertificatesStream.call()
                .map(certificate -> ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build())
                .flatMap(iotHelper::getAttachedThings));

        testNotMeaningfulWithout("things attached to certificates", numberOfAttachedThings);
    }

    @Test
    public void shouldListAttachedPoliciesWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<Certificate>> getCertificatesStream = () -> iotHelper.getCertificates();
        testNotMeaningfulWithout("certificates", getCertificatesStream.call());

        int numberOfAttachedThings = TestHelper.logAndCount(getCertificatesStream.call()
                .map(certificate -> ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build())
                .flatMap(iotHelper::getAttachedPolicies));

        testNotMeaningfulWithout("policies attached to certificates", numberOfAttachedThings);
    }

    @Test
    public void shouldListThingPrincipalsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<ThingAttribute>> getThingsStream = () -> iotHelper.getThings();
        testNotMeaningfulWithout("things", getThingsStream.call());

        int numberOfThingPrincipals = TestHelper.logAndCount(getThingsStream.call()
                .map(thingAttribute -> ImmutableThingName.builder().name(thingAttribute.thingName()).build())
                .flatMap(iotHelper::getThingPrincipals));

        testNotMeaningfulWithout("principals attached to things", numberOfThingPrincipals);
    }

    @Test
    public void shouldListJobsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<JobSummary>> getJobsStream = () -> iotHelper.getJobs();
        testNotMeaningfulWithout("jobs", getJobsStream.call());
    }

    @Test
    public void shouldListJobExecutionsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<JobSummary>> getJobsStream = () -> iotHelper.getJobs();
        testNotMeaningfulWithout("jobs", getJobsStream.call());

        JobSummary jobSummary = getJobsStream.call().get();
        Callable<Stream<JobExecutionSummaryForJob>> getJobsExecutionsStream = () -> iotHelper.getJobExecutions(jobSummary);

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
        Callable<Stream> streamCallable = () -> iotHelper.getThingsByGroupName(JUNKFORGROUPTESTING);
        waitForNonZeroFleetIndexResult(streamCallable);

        testNotMeaningfulWithout("things in thing groups", streamCallable.call());
    }

    @Test
    public void shouldThrowExceptionDueToTypeErasureAmbiguityWhenRequestingSearchIndexResults() {
        String queryString = String.join(FLEET_INDEXING_QUERY_STRING_DELIMITER, THING_GROUP_NAMES, "*");

        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString(queryString)
                .build();

        UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> new ResultsIterator<ThingDocument>(iotClient, searchIndexRequest).stream().size());
        assertThat(unsupportedOperationException.getMessage(), org.hamcrest.CoreMatchers.containsString("Multiple methods found"));
    }

    @Test
    public void shouldGenerateRandomRsaKeypair() {
        getRandomRsaKeyPair();
    }

    @Test
    public void shouldGenerateRandomEcKeypair() {
        getRandomEcKeyPair();
    }

    private java.security.KeyPair getRandomRsaKeyPair() {
        return new BasicIotHelper().getRandomRsaKeypair(RSA_KEY_SIZE);
    }

    private java.security.KeyPair getRandomEcKeyPair() {
        return new BasicIotHelper().getRandomEcKeypair(EC_KEY_SIZE);
    }

    @Test
    public void shouldGenerateCertificateSigningRequest() {
        iotHelper.generateCertificateSigningRequest(getRandomRsaKeyPair(), subjectNameBeforeSigning);
    }

    @Test
    public void shouldGenerateCsrWithExpectedSubject() {
        PKCS10CertificationRequest pkcs10CertificationRequest = iotHelper.generateCertificateSigningRequest(getRandomRsaKeyPair(), subjectNameBeforeSigning);

        assertThat(pkcs10CertificationRequest.getSubject().toString(), is(expectedCsrSubject));
    }

    @Test
    public void shouldGenerateRsaCertificateSigningRequestAndExtractPublicKey() {
        java.security.KeyPair randomRsaKeyPair = getRandomRsaKeyPair();
        RSAPublicKey expectedRsaPublicKey = (RSAPublicKey) randomRsaKeyPair.getPublic();
        PKCS10CertificationRequest pkcs10CertificationRequest = iotHelper.generateCertificateSigningRequest(randomRsaKeyPair, subjectNameBeforeSigning);
        RSAPublicKey rsaPublicKeyFromCsrPem = (RSAPublicKey) iotHelper.getPublicKeyFromCsrPem(iotHelper.toPem(pkcs10CertificationRequest));
        assertThat(expectedRsaPublicKey, is(rsaPublicKeyFromCsrPem));
    }

    @Test
    public void shouldGenerateEcCertificateSigningRequestAndExtractPublicKey() {
        java.security.KeyPair randomEcKeyPair = getRandomEcKeyPair();
        ECPublicKey expectedEcPublicKey = (ECPublicKey) randomEcKeyPair.getPublic();
        PKCS10CertificationRequest pkcs10CertificationRequest = iotHelper.generateCertificateSigningRequest(randomEcKeyPair, subjectNameBeforeSigning);
        ECPublicKey ecPublicKeyFromCsrPem = (ECPublicKey) iotHelper.getPublicKeyFromCsrPem(iotHelper.toPem(pkcs10CertificationRequest));
        assertThat(expectedEcPublicKey, is(ecPublicKeyFromCsrPem));
    }

    @Test
    public void shouldGenerateCertificateWithDifferentSubject() {
        PKCS10CertificationRequest pkcs10CertificationRequest = iotHelper.generateCertificateSigningRequest(getRandomRsaKeyPair(), subjectNameBeforeSigning);

        RSAPublicKey rsaPublicKeyFromCsrPem = (RSAPublicKey) iotHelper.getPublicKeyFromCsrPem(iotHelper.toPem(pkcs10CertificationRequest));
        X509Certificate x509Certificate = iotHelper.generateX509Certificate(rsaPublicKeyFromCsrPem, issuerName, subjectNameAfterSigning);

        String issuerPrincipalString = x509Certificate.getIssuerX500Principal().getName();
        String subjectPrincipalString = x509Certificate.getSubjectDN().getName();

        assertThat(issuerPrincipalString, containsString(COMMON_NAME_PREFIX));
        assertThat(subjectPrincipalString, containsString(COMMON_NAME_PREFIX));
        assertThat(subjectPrincipalString, containsString(String.join(SUBJECT_KEY_VALUE_SEPARATOR, IotHelper.O, CERTIFICATE_TEST_ORG)));
        assertThat(subjectPrincipalString, is(not(expectedCsrSubject)));
    }

    @Test
    public void shouldRegisterRsaCsr() {
        // Make sure that the CSRs we generate for RSA keys are valid
        createCsrFromKeyPairAndRegister(getRandomRsaKeyPair());
    }

    @Test
    public void shouldRegisterEcCsr() {
        // Make sure that the CSRs we generate for EC keys are valid
        createCsrFromKeyPairAndRegister(getRandomEcKeyPair());
    }

    private void createCsrFromKeyPairAndRegister(java.security.KeyPair randomRsaKeyPair) {
        PKCS10CertificationRequest pkcs10CertificationRequest = iotHelper.generateCertificateSigningRequest(randomRsaKeyPair, subjectNameBeforeSigning);

        String csr = iotHelper.toPem(pkcs10CertificationRequest);

        CreateCertificateFromCsrRequest createCertificateFromCsrRequest = CreateCertificateFromCsrRequest.builder()
                .certificateSigningRequest(csr)
                // Do not set it as active so we can delete it more easily later
                .setAsActive(false)
                .build();

        CreateCertificateFromCsrResponse createCertificateFromCsrResponse = iotClient.createCertificateFromCsr(createCertificateFromCsrRequest);

        // Make sure the subject return from AWS IoT matches what we expect
        X509CertificateHolder x509CertificateHolder = iotHelper.tryGetObjectFromPem(createCertificateFromCsrResponse.certificatePem(), X509CertificateHolder.class).get();
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
        PKCS10CertificationRequest pkcs10CertificationRequest = iotHelper.generateCertificateSigningRequest(randomRsaKeyPair, subjectNameBeforeSigning);

        RSAPublicKey rsaPublicKeyFromCsrPem = (RSAPublicKey) iotHelper.getPublicKeyFromCsrPem(iotHelper.toPem(pkcs10CertificationRequest));
        X509Certificate x509Certificate = iotHelper.generateX509Certificate(rsaPublicKeyFromCsrPem, issuerName, subjectNameAfterSigning);

        RegisterCertificateWithoutCaRequest registerCertificateWithoutCaRequest = RegisterCertificateWithoutCaRequest.builder()
                .certificatePem(iotHelper.toPem(x509Certificate))
                .build();

        RegisterCertificateWithoutCaResponse registerCertificateWithoutCaResponse = iotClient.registerCertificateWithoutCA(registerCertificateWithoutCaRequest);

        // Cleanup
        DeleteCertificateRequest deleteCertificateRequest = DeleteCertificateRequest.builder()
                .certificateId(registerCertificateWithoutCaResponse.certificateId())
                .build();

        iotClient.deleteCertificate(deleteCertificateRequest);
    }
}

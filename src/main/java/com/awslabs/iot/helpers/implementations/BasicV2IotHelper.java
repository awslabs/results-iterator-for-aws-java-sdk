package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIteratorAbstract;
import io.vavr.Tuple2;
import io.vavr.Value;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.Certificate;
import software.amazon.awssdk.services.iot.model.Policy;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

import javax.inject.Inject;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

public class BasicV2IotHelper implements V2IotHelper {
    public static final int RSA_SIGNER_KEY_SIZE = 4096;
    private static final KeyFactory rsaKeyFactory = Try.of(() -> KeyFactory.getInstance("RSA")).get();
    private final Logger log = LoggerFactory.getLogger(BasicV2IotHelper.class);

    @Inject
    IotClient iotClient;
    @Inject
    IotDataPlaneClient iotDataPlaneClient;

    @Inject
    public BasicV2IotHelper() {
    }

    @Override
    public String getEndpoint(V2IotEndpointType v2IotEndpointType) {
        return getEndpoint(v2IotEndpointType.getValue());
    }

    private String getEndpoint(String endpointType) {
        DescribeEndpointRequest describeEndpointRequest = DescribeEndpointRequest.builder()
                .endpointType(endpointType)
                .build();

        return iotClient.describeEndpoint(describeEndpointRequest).endpointAddress();
    }

    @Override
    public boolean certificateExists(CertificateId certificateId) {
        DescribeCertificateRequest describeCertificateRequest = DescribeCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .build();

        return Try.of(() -> iotClient.describeCertificate(describeCertificateRequest) != null)
                .recover(ResourceNotFoundException.class, throwable -> false)
                .get();
    }

    @Override
    public boolean policyExists(PolicyName policyName) {
        GetPolicyRequest getPolicyRequest = GetPolicyRequest.builder()
                .policyName(policyName.getName())
                .build();

        return Try.of(() -> iotClient.getPolicy(getPolicyRequest) != null)
                .recover(ResourceNotFoundException.class, throwable -> false)
                .get();
    }

    @Override
    public void createPolicyIfNecessary(PolicyName policyName, PolicyDocument document) {
        if (policyExists(policyName)) {
            return;
        }

        CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder()
                .policyName(policyName.getName())
                .policyDocument(document.getDocument())
                .build();

        iotClient.createPolicy(createPolicyRequest);
    }

    @Override
    public void attachPrincipalPolicy(PolicyName policyName, CertificateArn certificateArn) {
        AttachPolicyRequest attachPolicyRequest = AttachPolicyRequest.builder()
                .policyName(policyName.getName())
                .target(certificateArn.getArn())
                .build();

        iotClient.attachPolicy(attachPolicyRequest);
    }

    @Override
    public void attachThingPrincipal(ThingName thingName, CertificateArn certificateArn) {
        AttachThingPrincipalRequest attachThingPrincipalRequest = AttachThingPrincipalRequest.builder()
                .thingName(thingName.getName())
                .principal(certificateArn.getArn())
                .build();

        iotClient.attachThingPrincipal(attachThingPrincipalRequest);
    }

    @Override
    public Stream<ThingPrincipal> getThingPrincipals(ThingName thingName) {
        ListThingPrincipalsRequest listThingPrincipalsRequest = ListThingPrincipalsRequest.builder()
                .thingName(thingName.getName())
                .build();

        // ListThingPrincipals will throw an exception if the thing does not exist
        return Try.of(() -> new V2ResultsIterator<String>(iotClient, listThingPrincipalsRequest).stream())
                // ResourceNotFoundException is OK, other exceptions are not
                .recover(ResourceNotFoundException.class, throwable -> Stream.empty())
                // Throw all other exceptions here
                .get()
                // Convert the principals to the correct static type
                .map(principal -> ImmutableThingPrincipal.builder().principal(principal).build());
    }

    @Override
    public Option<ThingArn> getThingArn(ThingName thingName) {
        return describeThing(thingName)
                // At this point our try should be successful, even if the resource wasn't found, but the result may be none
                .map(DescribeThingResponse::thingArn)
                .map(thingArn -> ImmutableThingArn.builder().arn(thingArn).build());
    }

    public Option<DescribeThingResponse> describeThing(ThingName thingName) {
        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                .thingName(thingName.getName())
                .build();

        // DescribeThing will throw an exception if the thing does not exist
        return Try.of(() -> Option.of(iotClient.describeThing(describeThingRequest)))
                .recover(ResourceNotFoundException.class, throwable -> Option.none())
                .get();
    }

    @Override
    public String getCredentialProviderUrl() {
        return getEndpoint(V2IotEndpointType.CREDENTIAL_PROVIDER);
    }

    @Override
    public Try<CreateRoleAliasResponse> tryCreateRoleAlias(Role serviceRole, RoleAlias roleAlias) {
        CreateRoleAliasRequest createRoleAliasRequest = getCreateRoleAliasRequest(serviceRole, roleAlias);

        return createRoleAlias(createRoleAliasRequest);
    }

    private Try<CreateRoleAliasResponse> createRoleAlias(CreateRoleAliasRequest createRoleAliasRequest) {
        return Try.of(() -> iotClient.createRoleAlias(createRoleAliasRequest));
    }

    private CreateRoleAliasRequest getCreateRoleAliasRequest(Role serviceRole, RoleAlias roleAlias) {
        return CreateRoleAliasRequest.builder()
                .roleArn(serviceRole.arn())
                .roleAlias(roleAlias.getName())
                .build();
    }

    @Override
    public CreateRoleAliasResponse forceCreateRoleAlias(Role serviceRole, RoleAlias roleAlias) {
        CreateRoleAliasRequest createRoleAliasRequest = getCreateRoleAliasRequest(serviceRole, roleAlias);

        return createRoleAlias(createRoleAliasRequest)
                .recover(ResourceAlreadyExistsException.class, throwable -> deleteAndRecreateRoleAlias(roleAlias, createRoleAliasRequest))
                .get();
    }

    private CreateRoleAliasResponse deleteAndRecreateRoleAlias(RoleAlias roleAlias, CreateRoleAliasRequest createRoleAliasRequest) {
        // Already exists, delete it and try again
        DeleteRoleAliasRequest deleteRoleAliasRequest = DeleteRoleAliasRequest.builder()
                .roleAlias(roleAlias.getName())
                .build();

        iotClient.deleteRoleAlias(deleteRoleAliasRequest);

        return iotClient.createRoleAlias(createRoleAliasRequest);
    }

    @Override
    public CertificateArn signCsrAndReturnCertificateArn(CertificateSigningRequest certificateSigningRequest) {
        CreateCertificateFromCsrRequest createCertificateFromCsrRequest = CreateCertificateFromCsrRequest.builder()
                .certificateSigningRequest(certificateSigningRequest.getRequest())
                .setAsActive(true)
                .build();

        return ImmutableCertificateArn.builder()
                .arn(iotClient.createCertificateFromCsr(createCertificateFromCsrRequest).certificateArn())
                .build();
    }

    @Override
    public Option<CertificatePem> getCertificatePem(CertificateArn certificateArn) {
        CertificateId certificateId = getCertificateId(certificateArn);

        return getCertificatePem(certificateId);
    }

    @Override
    public Option<CertificatePem> getCertificatePem(CertificateId certificateId) {
        DescribeCertificateRequest describeCertificateRequest = DescribeCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .build();

        // DescribeCertificate will throw an exception if the certificate does not exist
        return Try.of(() -> Option.of(iotClient.describeCertificate(describeCertificateRequest)))
                .recover(ResourceNotFoundException.class, throwable -> Option.none())
                // At this point our try should be successful, even if the resource wasn't found, but the result may be none
                .get()
                .map(DescribeCertificateResponse::certificateDescription)
                .map(CertificateDescription::certificatePem)
                .map(pem -> ImmutableCertificatePem.builder().pem(pem).build());
    }

    @Override
    public ThingArn createThing(ThingName thingName) {
        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(thingName.getName())
                .build();

        return Try.of(() -> iotClient.createThing(createThingRequest).thingArn())
                .map(thingArnString -> ImmutableThingArn.builder().arn(thingArnString).build())
                .recover(ResourceAlreadyExistsException.class, throwable -> recoverFromResourceAlreadyExistsException(thingName, throwable))
                .get();
    }

    private ImmutableThingArn recoverFromResourceAlreadyExistsException(ThingName thingName, ResourceAlreadyExistsException throwable) {
        if (!throwable.getMessage().contains("with different attributes")) {
            throw new RuntimeException(throwable);
        }

        log.debug(String.join("", "The thing [", thingName.getName(), "] already exists with different tags/attributes (e.g. immutable or other attributes)"));

        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                .thingName(thingName.getName())
                .build();

        return ImmutableThingArn.builder().arn(iotClient.describeThing(describeThingRequest).thingArn()).build();
    }

    @Override
    public boolean isThingImmutable(ThingName thingName) {
        // Describe the thing by name
        return describeThing(thingName)
                // Extract the attributes
                .map(DescribeThingResponse::attributes)
                // Convert to a vavr hash map
                .map(HashMap::ofAll)
                // Extract the keys from the attributes
                .map(Map::keySet)
                // Turn it into a stream
                .map(Value::toStream)
                // Use an none stream if no values are present
                .getOrElse(Stream.empty())
                // Check if any of the keys are equal to the immutable string
                .exists(IMMUTABLE_ATTRIBUTE_NAME_OR_VALUE::equals);
    }

    @Override
    public boolean isAnyThingImmutable(Stream<ThingName> thingName) {
        return thingName.exists(this::isThingImmutable);
    }

    @Override
    public Stream<Certificate> getCertificates() {
        return new V2ResultsIterator<Certificate>(iotClient, ListCertificatesRequest.class).stream();
    }

    @Override
    public Stream<Certificate> getUnattachedCertificates() {
        return new V2ResultsIterator<Certificate>(iotClient, ListCertificatesRequest.class).stream()
                .filter(certificate -> !hasAttachedThings(certificate) && !hasAttachedPolicies(certificate));
    }

    private boolean hasAttachedThings(Certificate certificate) {
        return getAttachedThings(certificate).nonEmpty();
    }

    private boolean hasAttachedPolicies(Certificate certificate) {
        return getAttachedPolicies(certificate).nonEmpty();
    }

    @Override
    public Stream<Policy> getPolicies() {
        return new V2ResultsIterator<Policy>(iotClient, ListPoliciesRequest.class).stream();
    }

    @Override
    public Stream<TopicRuleListItem> getTopicRules() {
        return new V2ResultsIterator<TopicRuleListItem>(iotClient, ListTopicRulesRequest.class).stream();
    }

    @Override
    public Stream<Certificate> getCaCertificates() {
        return getCertificates()
                .filter(certificate -> isCaCertificate(certificate.certificateArn()));
    }

    @Override
    public Stream<ThingName> getAttachedThings(Certificate certificate) {
        return getAttachedThings(ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build());
    }

    @Override
    public Stream<ThingName> getAttachedThings(CertificateArn certificateArn) {
        ListPrincipalThingsRequest listPrincipalThingsRequest = ListPrincipalThingsRequest.builder()
                .principal(certificateArn.getArn())
                .build();

        return new V2ResultsIterator<String>(iotClient, listPrincipalThingsRequest).stream()
                .map(thingName -> ImmutableThingName.builder().name(thingName).build());
    }

    @Override
    public Stream<Policy> getAttachedPolicies(Certificate certificate) {
        return getAttachedPolicies(ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build());
    }

    @Override
    public Stream<Policy> getAttachedPolicies(CertificateArn certificateArn) {
        ListAttachedPoliciesRequest listAttachedPoliciesRequest = ListAttachedPoliciesRequest.builder()
                .target(certificateArn.getArn())
                .build();

        return new V2ResultsIterator<Policy>(iotClient, listAttachedPoliciesRequest).stream();
    }

    private CertificateId getCertificateId(CertificateArn certificateArn) {
        String certificateId = certificateArn.getArn().split("/")[1];

        return ImmutableCertificateId.builder().id(certificateId).build();
    }

    private boolean isCaCertificate(String principal) {
        return principal.contains(CACERT_IDENTIFIER);
    }

    @Override
    public boolean isCaCertificate(CertificateArn certificateArn) {
        return isCaCertificate(certificateArn.getArn());
    }

    @Override
    public void recursiveDelete(Certificate certificate) {
        recursiveDelete(ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build());
    }

    @Override
    public void recursiveDelete(CertificateArn certificateArn) {
        if (isCaCertificate(certificateArn)) {
            throw new RuntimeException("Recursive delete is not supported for CA certificates");
        }

        if (isAnyThingImmutable(getAttachedThings(certificateArn))) {
            log.debug(String.join("", "Skipping deletion of [", certificateArn.getArn(), "] because it is attached to at least one immutable thing"));
            return;
        }

        recursiveDeleteNonCaCertificate(certificateArn);
    }

    @Override
    public void deleteCaCertificate(Certificate certificate) {
        deleteCaCertificate(ImmutableCertificateArn.builder().arn(certificate.certificateArn()).build());
    }

    @Override
    public void deleteCaCertificate(CertificateArn certificateArn) {
        if (!isCaCertificate(certificateArn)) {
            throw new RuntimeException("Delete CA certificate can not be called with the certificate ARN of a non-CA certificate");
        }

        // This is a CA certificate, it just needs to be deactivated and removed
        CertificateId certificateId = getCertificateId(certificateArn);

        UpdateCaCertificateRequest updateCaCertificateRequest = UpdateCaCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .newStatus(CACertificateStatus.INACTIVE)
                .build();

        log.debug(String.join("", "Attempting to mark CA certificate inactive [", certificateId.getId(), "]"));
        iotClient.updateCACertificate(updateCaCertificateRequest);

        DeleteCaCertificateRequest deleteCaCertificateRequest = DeleteCaCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .build();

        log.debug(String.join("", "Attempting to delete CA certificate [", certificateId.getId(), "]"));
        iotClient.deleteCACertificate(deleteCaCertificateRequest);
    }

    @Override
    public void delete(Policy policy) {
        delete(ImmutablePolicyName.builder().name(policy.policyName()).build());
    }

    @Override
    public void delete(PolicyName policyName) {
        DeletePolicyRequest deletePolicyRequest = DeletePolicyRequest.builder()
                .policyName(policyName.getName())
                .build();

        iotClient.deletePolicy(deletePolicyRequest);
    }

    private void detach(CertificateArn certificateArn, Policy policy) {
        DetachPolicyRequest detachPolicyRequest = DetachPolicyRequest.builder()
                .target(certificateArn.getArn())
                .policyName(policy.policyName())
                .build();

        iotClient.detachPolicy(detachPolicyRequest);
    }

    private void detach(CertificateArn certificateArn, ThingName thingName) {
        DetachThingPrincipalRequest detachThingPrincipalRequest = DetachThingPrincipalRequest.builder()
                .principal(certificateArn.getArn())
                .thingName(thingName.getName())
                .build();

        iotClient.detachThingPrincipal(detachThingPrincipalRequest);
    }

    @Override
    public void delete(ThingName thingName) {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(thingName.getName())
                .build();

        iotClient.deleteThing(deleteThingRequest);
    }

    private void recursiveDeleteNonCaCertificate(CertificateArn certificateArn) {
        // This is a regular certificate
        CertificateId certificateId = getCertificateId(certificateArn);

        // Detach all policies from it
        getAttachedPolicies(certificateArn).forEach(policy -> detach(certificateArn, policy));

        // Delete the policies that were attached but aren't shared with other certificates (ignores failures)
        // getAttachedPolicies(certificateArn).forEach(policy -> Try.run(() -> delete(policy)));

        // Detach all things from it
        getAttachedThings(certificateArn).forEach(thingName -> detach(certificateArn, thingName));

        // Delete the things that were attached but aren't shared with other certificates (ignores failures)
        // getAttachedThings(certificateArn).forEach(thingName -> Try.run(() -> delete(thingName)));

        delete(certificateId);
    }

    @Override
    public void delete(Certificate certificate) {
        delete(ImmutableCertificateId.builder().id(certificate.certificateId()).build());
    }

    @Override
    public void delete(CertificateArn certificateArn) {
        delete(getCertificateId(certificateArn));
    }

    @Override
    public void delete(CertificateId certificateId) {
        // Mark the certificate as inactive
        UpdateCertificateRequest updateCertificateRequest = UpdateCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .newStatus(CertificateStatus.INACTIVE)
                .build();

        log.debug(String.join("", "Attempting to mark certificate inactive [", certificateId.getId(), "]"));
        iotClient.updateCertificate(updateCertificateRequest);

        DeleteCertificateRequest deleteCertificateRequest = DeleteCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .build();

        log.debug(String.join("", "Attempting to delete certificate [", certificateId.getId(), "]"));
        iotClient.deleteCertificate(deleteCertificateRequest);
    }

    @Override
    public Stream<ThingAttribute> getThings() {
        return new V2ResultsIterator<ThingAttribute>(iotClient, ListThingsRequest.class).stream();
    }

    @Override
    public Stream<GroupNameAndArn> getThingGroups() {
        return new V2ResultsIterator<GroupNameAndArn>(iotClient, ListThingGroupsRequest.class).stream();
    }

    @Override
    public void delete(GroupNameAndArn groupNameAndArn) {
        delete(ImmutableThingGroup.builder().name(groupNameAndArn.groupName()).build());
    }

    @Override
    public void delete(ThingGroup thingGroup) {
        DeleteThingGroupRequest deleteThingGroupRequest = DeleteThingGroupRequest.builder()
                .thingGroupName(thingGroup.getName())
                .build();

        iotClient.deleteThingGroup(deleteThingGroupRequest);
    }

    @Override
    public void createTopicRule(RuleName ruleName, TopicRulePayload topicRulePayload) {
        CreateTopicRuleRequest createTopicRuleRequest = CreateTopicRuleRequest.builder()
                .ruleName(ruleName.getName())
                .topicRulePayload(topicRulePayload)
                .build();

        iotClient.createTopicRule(createTopicRuleRequest);
    }

    @Override
    public void deleteTopicRule(RuleName ruleName) {
        DeleteTopicRuleRequest deleteTopicRuleRequest = DeleteTopicRuleRequest.builder()
                .ruleName(ruleName.getName())
                .build();

        iotClient.deleteTopicRule(deleteTopicRuleRequest);
    }

    @Override
    public void publish(TopicName topicName, Qos qos, String payload) {
        SdkBytes sdkBytes = SdkBytes.fromString(payload, Charset.defaultCharset());

        publish(topicName, qos, sdkBytes);
    }

    @Override
    public void publish(TopicName topicName, Qos qos, byte[] payload) {
        SdkBytes sdkBytes = SdkBytes.fromByteArray(payload);

        publish(topicName, qos, sdkBytes);
    }

    @Override
    public void publish(TopicName topicName, Qos qos, SdkBytes payload) {
        PublishRequest publishRequest = PublishRequest.builder()
                .topic(topicName.getName())
                .qos(qos.getLevel())
                .payload(payload)
                .build();

        iotDataPlaneClient.publish(publishRequest);
    }

    @Override
    public Stream<JobSummary> getJobs() {
        return new V2ResultsIterator<JobSummary>(iotClient, ListJobsRequest.class).stream();
    }

    @Override
    public void delete(JobSummary jobSummary) {
        DeleteJobRequest deleteJobRequest = DeleteJobRequest.builder()
                .jobId(jobSummary.jobId())
                .build();

        iotClient.deleteJob(deleteJobRequest);
    }

    @Override
    public void forceDelete(JobSummary jobSummary) {
        DeleteJobRequest deleteJobRequest = DeleteJobRequest.builder()
                .jobId(jobSummary.jobId())
                .force(true)
                .build();

        iotClient.deleteJob(deleteJobRequest);
    }

    @Override
    public Stream<JobExecutionSummaryForJob> getJobExecutions(JobSummary jobSummary) {
        ListJobExecutionsForJobRequest listJobExecutionsForJobRequest = ListJobExecutionsForJobRequest.builder()
                .jobId(jobSummary.jobId())
                .build();

        return new V2ResultsIterator<JobExecutionSummaryForJob>(iotClient, listJobExecutionsForJobRequest).stream();
    }

    @Override
    public Stream<ThingDocument> getThingsByGroupName(String groupName) {
        String queryString = String.join(FLEET_INDEXING_QUERY_STRING_DELIMITER, THING_GROUP_NAMES, groupName);

        SearchIndexRequest searchIndexRequest = SearchIndexRequest.builder()
                .queryString(queryString)
                .build();

        // Must be abstract so we can avoid type erasure get the type information for ThingDocument at runtime.
        //   Specifically this must be done because this API has two methods that return lists (ThingDocument
        //   and ThingGroupDocument)
        return Stream.ofAll(new V2ResultsIteratorAbstract<ThingDocument>(iotClient, searchIndexRequest) {
        }.stream());
    }

    @Override
    public <T> Try<T> tryGetObjectFromPem(File file, Class<T> returnClass) {
        return Try.of(() -> Files.readAllBytes(file.toPath()))
                .flatMap(data -> tryGetObjectFromPem(data, returnClass));
    }

    @Override
    public <T> Try<T> tryGetObjectFromPem(String pemString, Class<T> returnClass) {
        return tryGetObjectFromPem(pemString.getBytes(StandardCharsets.UTF_8), returnClass);
    }

    @Override
    public <T> Try<T> tryGetObjectFromPem(byte[] pemBytes, Class<T> returnClass) {
        return Try.withResources(() -> new ByteArrayInputStream(pemBytes))
                .of(InputStreamReader::new)
                .map(PEMParser::new)
                // Read the object from the PEM
                .mapTry(PEMParser::readObject)
                // Make sure it isn't NULL
                .filter(Objects::nonNull, () -> new RuntimeException("pemBytes could not be converted to the requested object type"))
                // Cast the object to the requested type
                .map(returnClass::cast);
    }

    @Override
    public RSAPublicKey getRsaPublicKeyFromCsrPem(String csrString) {
        return getRsaPublicKeyFromCsrPem(csrString.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public RSAPublicKey getRsaPublicKeyFromCsrPem(byte[] csrBytes) {
        return tryGetObjectFromPem(csrBytes, PKCS10CertificationRequest.class)
                // Extract the public key info
                .map(PKCS10CertificationRequest::getSubjectPublicKeyInfo)
                // Create an asymmetric key parameter object
                .mapTry(PublicKeyFactory::createKey)
                // Cast the asymmetric key parameter object to RSA key parameters
                .map(RSAKeyParameters.class::cast)
                // Create an RSA public key spec from the key parameters
                .map(rsaKeyParameters -> new RSAPublicKeySpec(rsaKeyParameters.getModulus(), rsaKeyParameters.getExponent()))
                // Generate an public key object from the public key spec
                .mapTry(rsaKeyFactory::generatePublic)
                // Cast the public key object to an RSA public key
                .map(RSAPublicKey.class::cast)
                // Throw an exception if any step fails, otherwise get the result
                .get();
    }

    @Override
    public X509Certificate generateX509Certificate(PublicKey publicKey, List<Tuple2<String, String>> signerName, List<Tuple2<String, String>> certificateName) {
        X500Name x500SignerName = toX500Name(signerName);
        X500Name x500CertificateName = toX500Name(certificateName);

        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

        // Valid from yesterday
        final Date start = Date.from(LocalDate.now().minus(1, ChronoUnit.DAYS).atStartOfDay().toInstant(ZoneOffset.UTC));

        // Until Dec 31st, 2049
        final Date until = Date.from(LocalDate.of(2049, Month.DECEMBER, 31).atStartOfDay().toInstant(ZoneOffset.UTC));

        final X509v3CertificateBuilder builder = new X509v3CertificateBuilder(x500SignerName,
                new BigInteger(10, new SecureRandom()),
                start,
                until,
                x500CertificateName,
                subPubKeyInfo
        );

        // Get an RSA signer
        ContentSigner rsaSigner = Try.of(() -> new JcaContentSignerBuilder(SHA_256_WITH_RSA)
                .setProvider(new BouncyCastleProvider())
                .build(getRandomRsaKeypair(RSA_SIGNER_KEY_SIZE).getPrivate())).get();

        final X509CertificateHolder holder = builder.build(rsaSigner);

        return Try.of(() -> new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider())
                .getCertificate(holder)).get();
    }

    @Override
    public String toPem(Object object) {
        StringWriter stringWriter = new StringWriter();

        // Throw exception on failure
        Try.withResources(() -> new JcaPEMWriter(stringWriter))
                .of(pemWriter -> writeObject(pemWriter, object))
                .get();

        return stringWriter.toString();
    }

    private Void writeObject(JcaPEMWriter writer, Object object) throws IOException {
        writer.writeObject(object);

        // Need to return NULL because Try.of(...) needs a return value. We cannot use Try.run(...) with
        //   Try.withResources(...).
        return null;
    }

    @Override
    public PKCS10CertificationRequest generateCertificateSigningRequest(KeyPair keyPair, List<Tuple2<String, String>> certificateName) {
        return generateCertificateSigningRequest(keyPair, certificateName, List.empty());
    }

    @Override
    public PKCS10CertificationRequest generateCertificateSigningRequest(KeyPair keyPair, List<Tuple2<String, String>> certificateName, List<Tuple2<ASN1ObjectIdentifier, ASN1Encodable>> attributes) {
        // Guidance from - https://stackoverflow.com/a/20550258
        X500Name x500CertificateName = toX500Name(certificateName);
        PKCS10CertificationRequestBuilder jcaPKCS10CertificationRequestBuilder = new JcaPKCS10CertificationRequestBuilder(x500CertificateName, keyPair.getPublic());

        // Add attributes if there are any
        attributes.forEach(attribute -> jcaPKCS10CertificationRequestBuilder.addAttribute(attribute._1, attribute._2));

        ContentSigner contentSigner = Try.of(() -> new JcaContentSignerBuilder(SHA_256_WITH_RSA)
                .build(keyPair.getPrivate()))
                .get();

        return jcaPKCS10CertificationRequestBuilder.build(contentSigner);
    }

    @Override
    public KeyPair getRandomRsaKeypair(int keySize) {
        KeyPairGenerator keyPairGenerator = Try.of(() -> KeyPairGenerator.getInstance("RSA")).get();
        keyPairGenerator.initialize(keySize, new SecureRandom());

        return keyPairGenerator.generateKeyPair();
    }

    private String javaCertificateToPem(java.security.cert.Certificate certificate) {
        return "-----BEGIN CERTIFICATE-----" +
                Base64.getEncoder().encodeToString(Try.of(certificate::getEncoded).get()) +
                "-----END CERTIFICATE-----";
    }

    @Override
    public String getFingerprint(java.security.cert.Certificate certificate) {
        return getFingerprint(javaCertificateToPem(certificate));
    }

    @Override
    public String getFingerprint(String pem) {
        // Get an X509CertificateHolder from the PEM string and then get the fingerprint from that, rethrow all exceptions
        return getFingerprint(tryGetObjectFromPem(pem, X509CertificateHolder.class).get());
    }

    @Override
    public String getFingerprint(X509CertificateHolder x509CertificateHolder) {
        // Get the DER encoded version of the certificate, rethrow all exceptions
        byte[] derEncodedCert = Try.of(x509CertificateHolder::getEncoded).get();

        // Get a message digester for SHA-256
        return Try.of(() -> MessageDigest.getInstance("SHA-256"))
                // Digest the DER encoded certificate data
                .map(messageDigest -> messageDigest.digest(derEncodedCert))
                // Turn it into a hex encoded string (actually an array of characters/bytes that represent the string)
                .map(Hex::encode)
                // Convert the array of characters to a string
                .map(String::new)
                // Throw an exception if anything fails and return the result to the caller
                .get();
    }

    private X500Name toX500Name(List<Tuple2<String, String>> input) {
        if (input.isEmpty()) {
            throw new RuntimeException("The list of input values can not be empty when generating an X500 name");
        }

        String data = input.map(tuple2 -> String.join(SUBJECT_KEY_VALUE_SEPARATOR, tuple2._1, tuple2._2)).collect(Collectors.joining(SUBJECT_ELEMENT_SEPARATOR));

        return new X500Name(data);
    }
}

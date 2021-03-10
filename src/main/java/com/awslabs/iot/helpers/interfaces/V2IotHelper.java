package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iot.data.*;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iot.model.*;

import java.io.File;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

public interface V2IotHelper {
    String FLEET_INDEXING_QUERY_STRING_DELIMITER = ":";
    String THING_GROUP_NAMES = "thingGroupNames";
    String SHA_256_WITH_RSA = "SHA256WithRSA";
    String SHA_256_WITH_ECDSA = "SHA256withECDSA";
    String SUBJECT_KEY_VALUE_SEPARATOR = "=";
    String SUBJECT_ELEMENT_SEPARATOR = ",";
    String IMMUTABLE_ATTRIBUTE_NAME_OR_VALUE = "immutable";
    String CACERT_IDENTIFIER = ":cacert/";
    String CN = "CN";
    String O = "O";

    String getEndpoint(V2IotEndpointType v2IotEndpointType);

    boolean certificateExists(CertificateId certificateId);

    boolean policyExists(PolicyName policyName);

    void createPolicyIfNecessary(PolicyName policyName, PolicyDocument policyDocument);

    void attachPrincipalPolicy(PolicyName policyName, CertificateArn certificateArn);

    void attachThingPrincipal(ThingName thingName, CertificateArn certificateArn);

    Stream<ThingPrincipal> getThingPrincipals(ThingName thingName);

    Option<ThingArn> getThingArn(ThingName thingName);

    String getCredentialProviderUrl();

    Try<CreateRoleAliasResponse> tryCreateRoleAlias(Role serviceRole, RoleAlias roleAlias);

    CreateRoleAliasResponse forceCreateRoleAlias(Role serviceRole, RoleAlias roleAlias);

    CertificateArn signCsrAndReturnCertificateArn(CertificateSigningRequest certificateSigningRequest);

    Option<CertificatePem> getCertificatePem(CertificateArn certificateArn);

    Option<CertificatePem> getCertificatePem(CertificateId certificateId);

    ThingArn createThing(ThingName thingName);

    boolean isThingImmutable(ThingName thingName);

    boolean isAnyThingImmutable(Stream<ThingName> thingName);

    Stream<Certificate> getCertificates();

    Stream<Certificate> getUnattachedCertificates();

    Stream<Policy> getPolicies();

    Stream<TopicRuleListItem> getTopicRules();

    Stream<Certificate> getCaCertificates();

    Stream<ThingName> getAttachedThings(Certificate certificate);

    Stream<ThingName> getAttachedThings(CertificateArn certificateArn);

    Stream<Policy> getAttachedPolicies(Certificate certificate);

    Stream<Policy> getAttachedPolicies(CertificateArn certificateArn);

    boolean isCaCertificate(CertificateArn certificateArn);

    void recursiveDelete(Certificate certificate);

    void recursiveDelete(CertificateArn certificateArn);

    void deleteCaCertificate(Certificate certificate);

    void deleteCaCertificate(CertificateArn certificateArn);

    void delete(Policy policy);

    void delete(PolicyName policyName);

    void delete(ThingName thingName);

    void delete(Certificate certificate);

    void delete(CertificateArn certificateArn);

    void delete(CertificateId certificateId);

    Stream<ThingAttribute> getThings();

    Stream<GroupNameAndArn> getThingGroups();

    void delete(GroupNameAndArn groupNameAndArn);

    void delete(ThingGroup thingGroup);

    void createTopicRule(RuleName ruleName, TopicRulePayload topicRulePayload);

    void deleteTopicRule(RuleName ruleName);

    void publish(TopicName topicName, Qos qos, String payload);

    void publish(TopicName topicName, Qos qos, byte[] payload);

    void publish(TopicName topicName, Qos qos, SdkBytes payload);

    Stream<JobSummary> getJobs();

    void delete(JobSummary jobSummary);

    void forceDelete(JobSummary jobSummary);

    Stream<JobExecutionSummaryForJob> getJobExecutions(JobSummary jobSummary);

    Stream<ThingDocument> getThingsByGroupName(String groupName);

    <T> Try<T> tryGetObjectFromPem(File file, Class<T> returnClass);

    <T> Try<T> tryGetObjectFromPem(String pemString, Class<T> returnClass);

    <T> Try<T> tryGetObjectFromPem(byte[] pemBytes, Class<T> returnClass);

    PublicKey getPublicKeyFromCsrPem(String csrString);

    PublicKey getPublicKeyFromCsrPem(byte[] csrBytes);

    X509Certificate generateX509Certificate(PublicKey publicKey, List<Tuple2<String, String>> signerName, List<Tuple2<String, String>> commonName);

    ContentSigner getRsaContentSigner(Option<java.security.KeyPair> keyPairOption);

    ContentSigner getEcdsaContentSigner(Option<java.security.KeyPair> keyPairOption);

    String toPem(Object object);

    PKCS10CertificationRequest generateCertificateSigningRequest(java.security.KeyPair keyPair, List<Tuple2<String, String>> certificateName);

    PKCS10CertificationRequest generateCertificateSigningRequest(java.security.KeyPair keyPair, List<Tuple2<String, String>> certificateName, List<Tuple2<ASN1ObjectIdentifier, ASN1Encodable>> attributes);

    java.security.KeyPair getRandomRsaKeypair(int keySize);

    java.security.KeyPair getRandomEcKeypair(int keySize);

    java.security.KeyPair getRandomEcdsaKeypair(int keySize);

    String getFingerprint(java.security.cert.Certificate certificate);

    String getFingerprint(X509CertificateHolder x509CertificateHolder);

    String getFingerprint(String pem);
}

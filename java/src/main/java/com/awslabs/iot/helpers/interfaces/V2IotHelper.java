package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iot.data.*;
import io.vavr.control.Try;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iot.model.*;

import java.util.Optional;
import java.util.stream.Stream;

public interface V2IotHelper {
    String IMMUTABLE = "immutable";
    String CACERT_IDENTIFIER = ":cacert/";
    String CERT_IDENTIFIER = ":cert/";

    String getEndpoint(V2IotEndpointType v2IotEndpointType);

    boolean certificateExists(CertificateId certificateId);

    boolean policyExists(PolicyName policyName);

    void createPolicyIfNecessary(PolicyName policyName, PolicyDocument policyDocument);

    void attachPrincipalPolicy(PolicyName policyName, CertificateArn certificateArn);

    void attachThingPrincipal(ThingName thingName, CertificateArn certificateArn);

    Stream<ThingPrincipal> getThingPrincipals(ThingName thingName);

    Optional<ThingArn> getThingArn(ThingName thingName);

    String getCredentialProviderUrl();

    Try<CreateRoleAliasResponse> tryCreateRoleAlias(Role serviceRole, RoleAlias roleAlias);

    CreateRoleAliasResponse forceCreateRoleAlias(Role serviceRole, RoleAlias roleAlias);

    CertificateArn signCsrAndReturnCertificateArn(CertificateSigningRequest certificateSigningRequest);

    Optional<CertificatePem> getCertificatePem(CertificateArn certificateArn);

    Optional<CertificatePem> getCertificatePem(CertificateId certificateId);

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
}

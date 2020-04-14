package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iot.data.*;
import io.vavr.control.Try;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iot.model.Certificate;
import software.amazon.awssdk.services.iot.model.CreateRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.Policy;
import software.amazon.awssdk.services.iot.model.ThingAttribute;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface V2IotHelper {
    String CACERT_IDENTIFIER = ":cacert/";
    String CERT_IDENTIFIER = ":cert/";

    String getEndpoint(V2IotEndpointType v2IotEndpointType);

    boolean certificateExists(CertificateId certificateId);

    boolean policyExists(PolicyName policyName);

    void createPolicyIfNecessary(PolicyName policyName, PolicyDocument policyDocument);

    void attachPrincipalPolicy(PolicyName policyName, CertificateArn certificateArn);

    void attachThingPrincipal(ThingName thingName, CertificateArn certificateArn);

    Optional<List<ThingPrincipal>> getThingPrincipals(ThingName thingName);

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

    Stream<Certificate> getCaCertificates();

    Stream<ThingName> getAttachedThings(CertificateArn certificateArn);

    Stream<Policy> getAttachedPolicies(CertificateArn certificateArn);

    boolean isCaCertificate(CertificateArn certificateArn);

    void recursiveDelete(Certificate certificate);

    void recursiveDelete(CertificateArn certificateArn);

    void deleteCaCertificate(Certificate certificate);

    void deleteCaCertificate(CertificateArn certificateArn);

    void delete(Certificate certificate);

    void delete(CertificateArn certificateArn);

    void delete(CertificateId certificateId);

    Stream<ThingAttribute> getThings();
}

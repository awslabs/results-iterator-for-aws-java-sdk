package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BasicV2IotHelper implements V2IotHelper {
    private final Logger log = LoggerFactory.getLogger(BasicV2IotHelper.class);

    @Inject
    IotClient iotClient;

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
    public Optional<List<ThingPrincipal>> getThingPrincipals(ThingName thingName) {
        ListThingPrincipalsRequest listThingPrincipalsRequest = ListThingPrincipalsRequest.builder()
                .thingName(thingName.getName())
                .build();

        // ListThingPrincipals will throw an exception if the thing does not exist
        return Optional.ofNullable(
                // Try to list the principals
                Try.of(() -> IotClient.create().listThingPrincipals(listThingPrincipalsRequest))
                        // ResourceNotFoundException is OK, other exceptions are not
                        .recover(ResourceNotFoundException.class, throwable -> null)
                        // Throw all other exceptions here
                        .get())
                // Get the principals
                .map(ListThingPrincipalsResponse::principals)
                // Get a stream from the principal list
                .map(principals -> principals.stream()
                        // Convert the principals to the correct static type
                        .map(principal -> ImmutableThingPrincipal.builder().principal(principal).build())
                        // Collect the principals to a list
                        .collect(Collectors.toList()));
    }

    @Override
    public Optional<ThingArn> getThingArn(ThingName thingName) {
        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                .thingName(thingName.getName())
                .build();

        // DescribeThing will throw an exception if the thing does not exist
        return Try.of(() -> Optional.of(iotClient.describeThing(describeThingRequest)))
                .recover(ResourceNotFoundException.class, throwable -> Optional.empty())
                // At this point our try should be successful, even if the resource wasn't found, but the result may be empty
                .get()
                .map(DescribeThingResponse::thingArn)
                .map(thingArn -> ImmutableThingArn.builder().arn(thingArn).build());
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
    public Optional<String> getCertificatePem(CertificateArn certificateArn) {
        String certificateId = certificateArn.getArn().split("/")[1];

        return getCertificatePem(ImmutableCertificateId.builder().id(certificateId).build());
    }

    @Override
    public Optional<String> getCertificatePem(CertificateId certificateId) {
        DescribeCertificateRequest describeCertificateRequest = DescribeCertificateRequest.builder()
                .certificateId(certificateId.getId())
                .build();

        // DescribeCertificate will throw an exception if the certificate does not exist
        return Try.of(() -> Optional.of(iotClient.describeCertificate(describeCertificateRequest)))
                .recover(ResourceNotFoundException.class, throwable -> Optional.empty())
                // At this point our try should be successful, even if the resource wasn't found, but the result may be empty
                .get()
                .map(DescribeCertificateResponse::certificateDescription)
                .map(CertificateDescription::certificatePem);
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
        if (throwable.getMessage().contains("with different attributes")) {
            log.info("The thing [" + thingName.getName() + "] already exists with different tags/attributes (e.g. immutable or other attributes)");

            DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                    .thingName(thingName.getName())
                    .build();

            return ImmutableThingArn.builder().arn(iotClient.describeThing(describeThingRequest).thingArn()).build();
        }

        throw new RuntimeException(throwable);
    }
}

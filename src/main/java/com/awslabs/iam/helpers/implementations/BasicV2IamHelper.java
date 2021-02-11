package com.awslabs.iam.helpers.implementations;

import com.awslabs.iam.data.*;
import com.awslabs.iam.helpers.interfaces.V2IamHelper;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import javax.inject.Inject;

public class BasicV2IamHelper implements V2IamHelper {
    private final Logger log = LoggerFactory.getLogger(BasicV2IamHelper.class);
    @Inject
    IamClient iamClient;
    @Inject
    StsClient stsClient;

    @Inject
    public BasicV2IamHelper() {
    }

    @Override
    public Option<Role> getRole(RoleName roleName) {
        GetRoleRequest getRoleRequest = GetRoleRequest.builder().roleName(roleName.getName()).build();

        return Option.of(Try.of(() -> iamClient.getRole(getRoleRequest).role())
                .recover(NoSuchEntityException.class, throwable -> null)
                .get());
    }

    @Override
    public Role createRoleIfNecessary(RoleName roleName, Option<AssumeRolePolicyDocument> optionalAssumeRolePolicyDocument) {
        Option<Role> optionalExistingRole = getRole(roleName);

        if (optionalExistingRole.isDefined()) {
            log.debug(String.join("", "Updating assume role policy for existing role [", roleName.getName(), "]"));
            UpdateAssumeRolePolicyRequest.Builder updateAssumeRolePolicyRequestBuilder = UpdateAssumeRolePolicyRequest.builder();
            updateAssumeRolePolicyRequestBuilder.roleName(roleName.getName());
            optionalAssumeRolePolicyDocument
                    .map(AssumeRolePolicyDocument::getDocument)
                    .forEach(updateAssumeRolePolicyRequestBuilder::policyDocument);

            iamClient.updateAssumeRolePolicy(updateAssumeRolePolicyRequestBuilder.build());

            return optionalExistingRole.get();
        }

        log.debug(String.join("", "Creating new role [", roleName.getName(), "]"));
        CreateRoleRequest.Builder createRoleRequestBuilder = CreateRoleRequest.builder();
        createRoleRequestBuilder.roleName(roleName.getName());
        optionalAssumeRolePolicyDocument
                .map(AssumeRolePolicyDocument::getDocument)
                .forEach(createRoleRequestBuilder::assumeRolePolicyDocument);

        CreateRoleResponse createRoleResponse = iamClient.createRole(createRoleRequestBuilder.build());

        return createRoleResponse.role();
    }

    @Override
    public void attachRolePolicies(Role role, Option<List<ManagedPolicyArn>> optionalManagedPolicyArns) {
        optionalManagedPolicyArns.forEach(policies -> policies.forEach(policy -> attachRolePolicy(role, policy)));
    }

    @Override
    public void putInlinePolicy(Role role, PolicyName policyName, Option<InlinePolicy> optionalInlinePolicy) {
        if (optionalInlinePolicy.isEmpty()) {
            return;
        }

        InlinePolicy inlinePolicy = optionalInlinePolicy.get();

        PutRolePolicyRequest putRolePolicyRequest = PutRolePolicyRequest.builder()
                .policyDocument(inlinePolicy.getPolicy())
                .policyName(policyName.getName())
                .roleName(role.roleName())
                .build();

        iamClient.putRolePolicy(putRolePolicyRequest);
    }

    @Override
    public void attachRolePolicy(Role role, ManagedPolicyArn managedPolicyArn) {
        attachRolePolicy(role, ImmutablePolicyArn.builder().arn(managedPolicyArn.getArn()).build());
    }

    @Override
    public void attachRolePolicy(Role role, PolicyArn policyArn) {
        AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                .roleName(role.roleName())
                .policyArn(policyArn.getArn())
                .build();

        iamClient.attachRolePolicy(attachRolePolicyRequest);
    }

    @Override
    public AccountId getAccountId() {
        return ImmutableAccountId.builder()
                .id(stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account())
                .build();
    }

    @Override
    public Stream<Role> getRoles() {
        return new V2ResultsIterator<Role>(iamClient, ListRolesRequest.class).stream();
    }

    @Override
    public Stream<RoleName> getRoleNames() {
        return getRoles()
                .map(role -> ImmutableRoleName.builder().name(role.roleName()).build());
    }
}

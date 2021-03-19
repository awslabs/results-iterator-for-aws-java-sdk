package com.awslabs.iam.helpers.interfaces;

import com.awslabs.iam.data.*;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import software.amazon.awssdk.services.iam.model.Role;

public interface IamHelper {
    Option<Role> getRole(RoleName roleName);

    Role createRoleIfNecessary(RoleName roleName, Option<AssumeRolePolicyDocument> assumeRolePolicyDocumentOption);

    void attachRolePolicies(Role role, Option<List<ManagedPolicyArn>> managedPolicyArnsOption);

    void putInlinePolicy(Role role, PolicyName policyName, Option<InlinePolicy> inlinePolicyOption);

    void attachRolePolicy(Role role, ManagedPolicyArn managedPolicyArn);

    void attachRolePolicy(Role role, PolicyArn policyArn);

    AccountId getAccountId();

    Stream<Role> getRoles();

    Stream<RoleName> getRoleNames();
}

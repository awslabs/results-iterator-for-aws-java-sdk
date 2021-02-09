package com.awslabs.iam.helpers.interfaces;

import com.awslabs.iam.data.*;
import io.vavr.control.Option;
import software.amazon.awssdk.services.iam.model.Role;

import java.util.List;
import java.util.stream.Stream;

public interface V2IamHelper {
    Option<Role> getRole(RoleName roleName);

    Role createRoleIfNecessary(RoleName roleName, Option<AssumeRolePolicyDocument> optionalAssumeRolePolicyDocument);

    void attachRolePolicies(Role role, Option<List<ManagedPolicyArn>> optionalManagedPolicyArns);

    void putInlinePolicy(Role role, PolicyName policyName, Option<InlinePolicy> optionalInlinePolicy);

    void attachRolePolicy(Role role, ManagedPolicyArn managedPolicyArn);

    void attachRolePolicy(Role role, PolicyArn policyArn);

    AccountId getAccountId();

    Stream<Role> getRoles();

    Stream<RoleName> getRoleNames();
}

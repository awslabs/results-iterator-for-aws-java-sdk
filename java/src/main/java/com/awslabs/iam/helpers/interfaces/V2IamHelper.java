package com.awslabs.iam.helpers.interfaces;

import com.awslabs.iam.data.*;
import software.amazon.awssdk.services.iam.model.Role;

import java.util.List;
import java.util.Optional;

public interface V2IamHelper {
    Optional<Role> getRole(RoleName roleName);

    Role createRoleIfNecessary(RoleName roleName, Optional<AssumeRolePolicyDocument> optionalAssumeRolePolicyDocument);

    void attachRolePolicies(Role role, Optional<List<ManagedPolicyArn>> optionalManagedPolicyArns);

    void putInlinePolicy(Role role, PolicyName policyName, Optional<InlinePolicy> optionalInlinePolicy);

    void attachRolePolicy(Role role, ManagedPolicyArn managedPolicyArn);

    void attachRolePolicy(Role role, PolicyArn policyArn);

    AccountId getAccountId();
}

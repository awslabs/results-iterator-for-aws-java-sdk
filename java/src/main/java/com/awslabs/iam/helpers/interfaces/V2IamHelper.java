package com.awslabs.iam.helpers.interfaces;

import software.amazon.awssdk.services.iam.model.Role;

import java.util.List;
import java.util.Optional;

public interface V2IamHelper {
    Optional<Role> getRole(String name);

    Role createRoleIfNecessary(String name, Optional<String> optionalAssumeRolePolicyDocument);

    void attachRolePolicies(Role role, Optional<List<String>> optionalManagedPolicyArns);

    void putInlinePolicy(Role role, String inlinePolicyName, Optional<String> optionalInlinePolicy);

    void attachRolePolicy(Role role, String policyArn);

    String getAccountId();
}

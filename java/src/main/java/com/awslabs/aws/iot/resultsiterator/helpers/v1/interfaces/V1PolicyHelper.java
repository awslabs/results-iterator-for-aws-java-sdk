package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.Policy;

import java.util.stream.Stream;

public interface V1PolicyHelper {
    void createAllowAllPolicy(String clientName);

    void attachPolicyToCertificate(String clientName, String certificateArn);

    Stream<Policy> listPolicies();

    Stream<String> listPolicyNames();

    Stream<String> listPolicyPrincipals(String policyName);

    void deletePolicy(String policyName);

    void detachPolicy(String principal, String policyName);

    Stream<Policy> listPrincipalPolicies(String principal);
}

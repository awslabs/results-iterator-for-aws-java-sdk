package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.Policy;

import java.util.List;

public interface V1PolicyHelper {
    void createAllowAllPolicy(String clientName);

    void attachPolicyToCertificate(String clientName, String certificateArn);

    List<Policy> listPolicies();

    List<String> listPolicyNames();

    List<String> listPolicyPrincipals(String policyName);

    void deletePolicy(String policyName);

    void detachPolicy(String principal, String policyName);

    List<Policy> listPrincipalPolicies(String principal);
}

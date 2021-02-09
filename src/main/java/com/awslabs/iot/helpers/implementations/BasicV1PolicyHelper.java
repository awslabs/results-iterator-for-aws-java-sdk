package com.awslabs.iot.helpers.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.*;
import com.awslabs.iot.helpers.interfaces.V1PolicyHelper;
import com.awslabs.resultsiterator.v1.implementations.V1ResultsIterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.slf4j.Logger;

import javax.inject.Inject;

public class BasicV1PolicyHelper implements V1PolicyHelper {
    private static final String ALLOW_ALL_POLICY_DOCUMENT = "{ \"Statement\": [ { \"Action\": \"iot:*\", \"Resource\": \"*\", \"Effect\": \"Allow\" } ], \"Version\": \"2012-10-17\" }";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1PolicyHelper.class);

    @Inject
    AWSIotClient awsIotClient;

    @Inject
    public BasicV1PolicyHelper() {
    }

    @Override
    public void createAllowAllPolicy(String clientName) {
        try {
            CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest()
                    .withPolicyName(clientName)
                    .withPolicyDocument(ALLOW_ALL_POLICY_DOCUMENT);
            awsIotClient.createPolicy(createPolicyRequest);
        } catch (Exception e) {
            log.debug("Failed to create the policy.  Maybe it already exists?  If not, do you have the correct permissions to call iot:CreatePolicy?");
            log.debug("Continuing anyway...");
        }
    }

    @Override
    public void attachPolicyToCertificate(String clientName, String certificateArn) {
        try {
            AttachPolicyRequest attachPolicyRequest = new AttachPolicyRequest()
                    .withPolicyName(clientName)
                    .withTarget(certificateArn);
            awsIotClient.attachPolicy(attachPolicyRequest);
        } catch (Exception e) {
            log.debug("Failed to attach the policy to your certificate.  Do you have the correct permissions to call iot:AttachPrincipalPolicy?");
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public Stream<Policy> listPolicies() {
        return new V1ResultsIterator<Policy>(awsIotClient, ListPoliciesRequest.class).stream();
    }

    @Override
    public Stream<String> listPolicyNames() {
        return listPolicies()
                .map(Policy::getPolicyName);
    }

    @Override
    public Stream<String> listPolicyPrincipals(String policyName) {
        ListPolicyPrincipalsRequest listPolicyPrincipalsRequest = new ListPolicyPrincipalsRequest()
                .withPolicyName(policyName);

        return new V1ResultsIterator<String>(awsIotClient, listPolicyPrincipalsRequest).stream();
    }

    @Override
    public void deletePolicy(String policyName) {
        listPolicyPrincipals(policyName)
                .forEach(policyPrincipal -> detachPolicy(policyPrincipal, policyName));

        DeletePolicyRequest deletePolicyRequest = new DeletePolicyRequest()
                .withPolicyName(policyName);

        try {
            log.debug(String.join("", "Attempting to delete policy [", policyName, "]"));
            awsIotClient.deletePolicy(deletePolicyRequest);
        } catch (UnauthorizedException e) {
            log.debug(String.join("", "You are not allowed to delete policy [", policyName, "]"));
        } catch (ResourceNotFoundException e) {
            log.debug(String.join("", "The policy was not found [", policyName, "]"));
        } catch (DeleteConflictException e) {
            log.debug("Policy has multiple versions, attempting to delete all versions");

            deletePolicyAndPolicyVersions(policyName);
        }
    }

    private void deletePolicyAndPolicyVersions(String policyName) {
        ListPolicyVersionsRequest listPolicyVersionsRequest = new ListPolicyVersionsRequest()
                .withPolicyName(policyName);

        ListPolicyVersionsResult listPolicyVersionsResult = awsIotClient.listPolicyVersions(listPolicyVersionsRequest);
        List<PolicyVersion> policyVersions = List.ofAll(listPolicyVersionsResult.getPolicyVersions());

        // Delete the policies
        for (PolicyVersion policyVersion : policyVersions) {
            if (policyVersion.isDefaultVersion()) {
                // Default policy, can't delete it
                continue;
            }

            DeletePolicyVersionRequest deletePolicyVersionRequest = new DeletePolicyVersionRequest()
                    .withPolicyVersionId(policyVersion.getVersionId())
                    .withPolicyName(policyName);
            awsIotClient.deletePolicyVersion(deletePolicyVersionRequest);
        }

        // Delete the policy
        DeletePolicyRequest deletePolicyRequest = new DeletePolicyRequest()
                .withPolicyName(policyName);

        awsIotClient.deletePolicy(deletePolicyRequest);
    }

    @Override
    public void detachPolicy(String principal, String policyName) {
        DetachPolicyRequest detachPolicyRequest = new DetachPolicyRequest()
                .withTarget(principal)
                .withPolicyName(policyName);

        log.debug(String.join("", "Attempting to detach principal [", principal, "] from policy [", policyName, "]"));
        awsIotClient.detachPolicy(detachPolicyRequest);
    }

    @Override
    public Stream<Policy> listPrincipalPolicies(String principal) {
        ListPrincipalPoliciesRequest listPrincipalPoliciesRequest = new ListPrincipalPoliciesRequest()
                .withPrincipal(principal);

        return new V1ResultsIterator<Policy>(awsIotClient, listPrincipalPoliciesRequest).stream();
    }
}

package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.*;
import com.awslabs.aws.iot.resultsiterator.exceptions.ThingAttachedToPrincipalsException;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1CertificateHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1PolicyHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1ThingHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class BasicV1ThingHelper implements V1ThingHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1ThingHelper.class);
    @Inject
    AWSIotClient awsIotClient;
    @Inject
    Provider<V1PolicyHelper> policyHelperProvider;

    @Inject
    public BasicV1ThingHelper() {
    }

    @Override
    public Stream<String> listThingNames() {
        return listThingAttributes().map(ThingAttribute::getThingName);
    }

    @Override
    public Stream<ThingAttribute> listThingAttributes() {
        return new V1ResultsIterator<ThingAttribute>(awsIotClient, ListThingsRequest.class).resultStream();
    }

    @Override
    public void delete(String thingName) throws ThingAttachedToPrincipalsException {
        DeleteThingRequest deleteThingRequest = new DeleteThingRequest()
                .withThingName(thingName);

        try {
            log.info("Attempting to delete thing [" + thingName + "]");
            awsIotClient.deleteThing(deleteThingRequest);
        } catch (InvalidRequestException e) {
            if (e.getMessage().contains(stillAttachedMessage(thingName))) {
                log.info("Thing [" + thingName + "] is still attached to principals");
                throw new ThingAttachedToPrincipalsException();
            }
        }
    }

    @Override
    public List<String> listPrincipals(String thingName) {
        // NOTE: Not paginated, always returns complete result set.  ResultsIterator not needed.
        ListThingPrincipalsRequest listThingPrincipalsRequest = new ListThingPrincipalsRequest()
                .withThingName(thingName);

        log.info("Attempting to list thing principals for [" + thingName + "]");
        ListThingPrincipalsResult listThingPrincipalsResult = awsIotClient.listThingPrincipals(listThingPrincipalsRequest);

        return listThingPrincipalsResult.getPrincipals();
    }

    @Override
    public Stream<String> listPrincipalThings(String principal) {
        ListPrincipalThingsRequest listPrincipalThingsRequest = new ListPrincipalThingsRequest()
                .withPrincipal(principal);

        return new V1ResultsIterator<String>(awsIotClient, listPrincipalThingsRequest).resultStream();
    }

    @Override
    public void detachPrincipal(String thingName, String principal) {
        DetachThingPrincipalRequest detachThingPrincipalRequest = new DetachThingPrincipalRequest()
                .withThingName(thingName)
                .withPrincipal(principal);

        log.info("Attempting to detach principal [" + principal + "] from [" + thingName + "]");
        awsIotClient.detachThingPrincipal(detachThingPrincipalRequest);
    }

    @Override
    public List<String> detachPrincipals(String thingName) {
        List<String> principals = listPrincipals(thingName);
        List<String> detachedPrincipals = new ArrayList<>();

        for (String principal : principals) {
            try {
                detachPrincipal(thingName, principal);
                detachedPrincipals.add(principal);
            } catch (UnauthorizedException e) {
                log.info("Could not detach principal [" + principal + "] from [" + thingName + "]");
            }
        }

        return detachedPrincipals;
    }

    @Override
    public void deletePrincipal(String principal) {
        String certificateId = principal.substring(principal.lastIndexOf('/') + 1);

        if (!principal.contains(V1CertificateHelper.CACERT_IDENTIFIER)) {
            if (principalAttachedToImmutableThing(principal)) {
                log.info("Skipping principal [" + principal + "] because it is attached to an immutable thing");
                return;
            }

            // This is a regular certificate, detach everything from it
            policyHelperProvider.get().listPrincipalPolicies(principal)
                    .forEach(policy -> detachAndDelete(principal, policy));

            listPrincipalThings(principal)
                    .forEach(thing -> detachPrincipal(thing, principal));

            UpdateCertificateRequest updateCertificateRequest = new UpdateCertificateRequest()
                    .withCertificateId(certificateId)
                    .withNewStatus(CertificateStatus.INACTIVE);

            log.info("Attempting to mark certificate inactive [" + certificateId + "]");
            awsIotClient.updateCertificate(updateCertificateRequest);

            DeleteCertificateRequest deleteCertificateRequest = new DeleteCertificateRequest()
                    .withCertificateId(certificateId);

            log.info("Attempting to delete certificate [" + certificateId + "]");
            awsIotClient.deleteCertificate(deleteCertificateRequest);
        } else {
            // This is a CA certificate, it just needs to be deactivated and removed
            UpdateCACertificateRequest updateCaCertificateRequest = new UpdateCACertificateRequest()
                    .withCertificateId(certificateId)
                    .withNewStatus(CACertificateStatus.INACTIVE);

            log.info("Attempting to mark CA certificate inactive [" + certificateId + "]");
            awsIotClient.updateCACertificate(updateCaCertificateRequest);

            DeleteCACertificateRequest deleteCaCertificateRequest = new DeleteCACertificateRequest()
                    .withCertificateId(certificateId);

            log.info("Attempting to delete CA certificate [" + certificateId + "]");
            awsIotClient.deleteCACertificate(deleteCaCertificateRequest);
        }
    }

    private void detachAndDelete(String principal, Policy policy) {
        String policyName = policy.getPolicyName();
        policyHelperProvider.get().detachPolicy(principal, policyName);
        policyHelperProvider.get().deletePolicy(policy.getPolicyName());
    }

    @Override
    public boolean principalAttachedToImmutableThing(String principal) {
        // Look for a true value
        Optional<Boolean> optionalBoolean = listPrincipalThings(principal)
                .map(this::isThingImmutable)
                .filter(value -> value)
                .findFirst();

        // If one is present then this thing is immutable
        return optionalBoolean.isPresent();
    }

    @Override
    public boolean isThingImmutable(String thingName) {
        DescribeThingRequest describeThingRequest = new DescribeThingRequest()
                .withThingName(thingName);

        DescribeThingResult describeThingResult = awsIotClient.describeThing(describeThingRequest);

        return describeThingResult.getAttributes().containsKey("immutable");
    }

    @Override
    public boolean isThingArnImmutable(String thingArn) {
        Optional<ThingAttribute> thingAttribute = getThingIfItExists(thingArn);

        if (!thingAttribute.isPresent()) {
            return false;
        }

        String thingName = thingAttribute.get().getThingName();

        return isThingImmutable(thingName);
    }

    @Override
    public Optional<ThingAttribute> getThingIfItExists(String thingArn) {
        return listThingAttributes()
                .filter(t -> t.getThingArn().equals(thingArn))
                .findFirst();
    }

    private String stillAttachedMessage(String thingName) {
        return "Thing " + thingName + " is still attached to one or more principals";
    }
}

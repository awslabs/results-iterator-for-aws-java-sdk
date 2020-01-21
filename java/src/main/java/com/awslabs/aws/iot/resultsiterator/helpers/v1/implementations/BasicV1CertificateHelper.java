package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.*;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.data.CertificateIdFilename;
import com.awslabs.aws.iot.resultsiterator.data.ClientCertFilename;
import com.awslabs.aws.iot.resultsiterator.data.ClientPrivateKeyFilename;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.IoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1CertificateHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1PolicyHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1ThingHelper;
import io.vavr.control.Try;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class BasicV1CertificateHelper implements V1CertificateHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1CertificateHelper.class);
    @Inject
    AWSIotClient awsIotClient;
    @Inject
    IoHelper IoHelper;
    @Inject
    Provider<V1ThingHelper> thingHelperProvider;
    @Inject
    Provider<V1PolicyHelper> policyHelperProvider;

    @Inject
    public BasicV1CertificateHelper() {
    }

    @Override
    public CreateKeysAndCertificateResult setupCertificate(ClientPrivateKeyFilename clientPrivateKeyFilename, ClientCertFilename clientCertFilename, CertificateIdFilename certificateIdFilename) {
        return Try.of(() -> {
            CreateKeysAndCertificateRequest createKeysAndCertificateRequest = new CreateKeysAndCertificateRequest()
                    .withSetAsActive(true);
            CreateKeysAndCertificateResult createKeysAndCertificateResult = awsIotClient.createKeysAndCertificate(createKeysAndCertificateRequest);

            IoHelper.writeFile(clientPrivateKeyFilename.getClientPrivateKeyFilename(), createKeysAndCertificateResult.getKeyPair().getPrivateKey());
            IoHelper.writeFile(clientCertFilename.getClientCertFilename(), createKeysAndCertificateResult.getCertificatePem());
            IoHelper.writeFile(certificateIdFilename.getCertificateIdFilename(), createKeysAndCertificateResult.getCertificateId());

            return createKeysAndCertificateResult;
        })
                .recover(Exception.class, throwable -> {
                    log.info("Failed to create the keys and certificate.  Do you have the correct permissions to call iot:CreateKeysAndCertificate?");
                    throw new RuntimeException(throwable);
                })
                .get();
    }

    @Override
    public List<Certificate> listCertificates() {
        List<Certificate> certificates = new V1ResultsIterator<Certificate>(awsIotClient, ListCertificatesRequest.class).iterateOverResults();

        return certificates;
    }

    @Override
    public List<CACertificate> listCaCertificates() {
        List<CACertificate> certificates = new V1ResultsIterator<CACertificate>(awsIotClient, ListCACertificatesRequest.class).iterateOverResults();

        return certificates;
    }

    @Override
    public List<String> listCaCertificateIds() {
        List<CACertificate> caCertificates = listCaCertificates();

        List<String> caCertificateIds = new ArrayList<>();

        for (CACertificate caCertificate : caCertificates) {
            caCertificateIds.add(caCertificate.getCertificateId());
        }

        return caCertificateIds;
    }

    @Override
    public List<String> listCaCertificateArns() {
        List<CACertificate> caCertificates = listCaCertificates();

        List<String> caCertificateArns = new ArrayList<>();

        for (CACertificate caCertificate : caCertificates) {
            // Note: API appears to return a ":cert/" ARN when it should return a ":cacert/" ARN, we fix this
            caCertificateArns.add(caCertificate.getCertificateArn().replace(V1CertificateHelper.CERT_IDENTIFIER, V1CertificateHelper.CACERT_IDENTIFIER));
        }

        return caCertificateArns;
    }

    @Override
    public List<String> getUnattachedCertificateArns() {
        List<String> certificateArns = listCertificateArns();

        List<String> unattachedCertificateArns = new ArrayList<>();

        for (String certificateArn : certificateArns) {
            List<String> principalThings = thingHelperProvider.get().listPrincipalThings(certificateArn);

            if (principalThings.size() != 0) {
                continue;
            }

            List<Policy> principalPolicies = policyHelperProvider.get().listPrincipalPolicies(certificateArn);

            if (principalPolicies.size() != 0) {
                continue;
            }

            unattachedCertificateArns.add(certificateArn);
        }

        return unattachedCertificateArns;
    }

    @Override
    public void attachCertificateToThing(String certificateArn, String thingName) {
        awsIotClient.attachThingPrincipal(new AttachThingPrincipalRequest()
                .withPrincipal(certificateArn)
                .withThingName(thingName));
    }

    @Override
    public List<String> listCertificateIds() {
        List<Certificate> certificates = listCertificates();

        List<String> certificateIds = new ArrayList<>();

        for (Certificate certificate : certificates) {
            certificateIds.add(certificate.getCertificateId());
        }

        return certificateIds;
    }

    @Override
    public List<String> listCertificateArns() {
        List<Certificate> certificates = listCertificates();

        List<String> certificateArns = new ArrayList<>();

        for (Certificate certificate : certificates) {
            certificateArns.add(certificate.getCertificateArn());
        }

        return certificateArns;
    }
}

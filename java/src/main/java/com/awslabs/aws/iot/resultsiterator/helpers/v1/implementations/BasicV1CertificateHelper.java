package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.*;
import com.awslabs.aws.iot.resultsiterator.data.CertificateIdFilename;
import com.awslabs.aws.iot.resultsiterator.data.ClientCertFilename;
import com.awslabs.aws.iot.resultsiterator.data.ClientPrivateKeyFilename;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.IoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1CertificateHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1PolicyHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1ThingHelper;
import io.vavr.control.Try;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.stream.Stream;

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
    public Stream<Certificate> listCertificates() {
        return new V1ResultsIterator<Certificate>(awsIotClient, ListCertificatesRequest.class).stream();
    }

    @Override
    public Stream<CACertificate> listCaCertificates() {
        return new V1ResultsIterator<CACertificate>(awsIotClient, ListCACertificatesRequest.class).stream();
    }

    @Override
    public Stream<String> listCaCertificateIds() {
        return listCaCertificates()
                .map(CACertificate::getCertificateId);
    }

    @Override
    public Stream<String> listCaCertificateArns() {
        // Note: API appears to return a ":cert/" ARN when it should return a ":cacert/" ARN, we fix this
        return listCaCertificates()
                .map(CACertificate::getCertificateArn)
                .map(arn -> arn.replace(V1CertificateHelper.CERT_IDENTIFIER, V1CertificateHelper.CACERT_IDENTIFIER));
    }

    @Override
    public Stream<String> getUnattachedCertificateArns() {
        return listCertificateArns()
                // Look for certificates with no things attached
                .filter(certificateArn -> !thingHelperProvider.get().listPrincipalThings(certificateArn).findAny().isPresent())
                // Look for certificates with no policies attached
                .filter(certificateArn -> !policyHelperProvider.get().listPrincipalPolicies(certificateArn).findAny().isPresent());
    }

    @Override
    public void attachCertificateToThing(String certificateArn, String thingName) {
        awsIotClient.attachThingPrincipal(new AttachThingPrincipalRequest()
                .withPrincipal(certificateArn)
                .withThingName(thingName));
    }

    @Override
    public Stream<String> listCertificateIds() {
        return listCertificates()
                .map(Certificate::getCertificateId);
    }

    @Override
    public Stream<String> listCertificateArns() {
        return listCertificates()
                .map(Certificate::getCertificateArn);
    }
}

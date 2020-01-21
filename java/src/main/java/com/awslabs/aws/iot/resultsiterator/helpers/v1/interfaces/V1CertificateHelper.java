package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.CACertificate;
import com.amazonaws.services.iot.model.Certificate;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.awslabs.aws.iot.resultsiterator.data.CertificateIdFilename;
import com.awslabs.aws.iot.resultsiterator.data.ClientCertFilename;
import com.awslabs.aws.iot.resultsiterator.data.ClientPrivateKeyFilename;

import java.util.List;

public interface V1CertificateHelper {
    String CACERT_IDENTIFIER = ":cacert/";
    String CERT_IDENTIFIER = ":cert/";

    CreateKeysAndCertificateResult setupCertificate(ClientPrivateKeyFilename clientPrivateKeyFilename, ClientCertFilename clientCertFilename, CertificateIdFilename certificateIdFilename);

    List<Certificate> listCertificates();

    List<String> listCertificateIds();

    List<String> listCertificateArns();

    List<CACertificate> listCaCertificates();

    List<String> listCaCertificateIds();

    List<String> listCaCertificateArns();

    List<String> getUnattachedCertificateArns();

    void attachCertificateToThing(String certificateArn, String thingName);
}

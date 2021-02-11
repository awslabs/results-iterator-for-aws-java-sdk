package com.awslabs.iot.helpers.interfaces;

import com.amazonaws.services.iot.model.CACertificate;
import com.amazonaws.services.iot.model.Certificate;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.awslabs.iot.data.CertificateIdFilename;
import com.awslabs.iot.data.ClientCertFilename;
import com.awslabs.iot.data.ClientPrivateKeyFilename;
import io.vavr.collection.Stream;

public interface V1CertificateHelper {
    String CACERT_IDENTIFIER = ":cacert/";
    String CERT_IDENTIFIER = ":cert/";

    CreateKeysAndCertificateResult setupCertificate(ClientPrivateKeyFilename clientPrivateKeyFilename, ClientCertFilename clientCertFilename, CertificateIdFilename certificateIdFilename);

    Stream<Certificate> listCertificates();

    Stream<String> listCertificateIds();

    Stream<String> listCertificateArns();

    Stream<CACertificate> listCaCertificates();

    Stream<String> listCaCertificateIds();

    Stream<String> listCaCertificateArns();

    Stream<String> getUnattachedCertificateArns();

    void attachCertificateToThing(String certificateArn, String thingName);
}

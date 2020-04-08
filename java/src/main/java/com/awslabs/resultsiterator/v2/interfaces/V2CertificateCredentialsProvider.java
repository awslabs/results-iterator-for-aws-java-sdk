package com.awslabs.resultsiterator.v2.interfaces;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public interface V2CertificateCredentialsProvider extends AwsCredentialsProvider {
    String AWS_CREDENTIAL_PROVIDER_PROPERTIES_FILE = "AWS_CREDENTIAL_PROVIDER_PROPERTIES_FILE";
    String AWS_CREDENTIAL_PROVIDER_URL = "AWS_CREDENTIAL_PROVIDER_URL";
    String AWS_THING_NAME = "AWS_THING_NAME";
    String AWS_ROLE_ALIAS = "AWS_ROLE_ALIAS";
    String AWS_CA_CERT_FILENAME = "AWS_CA_CERT_FILENAME";
    String AWS_CLIENT_CERT_FILENAME = "AWS_CLIENT_CERT_FILENAME";
    String AWS_CLIENT_PRIVATE_KEY_FILENAME = "AWS_CLIENT_PRIVATE_KEY_FILENAME";
    String AWS_CLIENT_PRIVATE_KEY_PASSWORD = "AWS_CLIENT_PRIVATE_KEY_PASSWORD";
    String X_AMZN_IOT_THINGNAME = "x-amzn-iot-thingname";
}

package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.resultsiterator.interfaces.SslContextHelper;
import com.awslabs.resultsiterator.v2.V2HelperModule;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import dagger.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.s3.S3Client;

import javax.inject.Singleton;

@Singleton
@Component(modules = {V2HelperModule.class})
public interface V2TestInjector {
    IotClient iotClient();

    GreengrassClient greengrassClient();

    V2S3Helper v2S3Helper();

    S3Client s3Client();

    V2CertificateCredentialsProvider v2CertificateCredentialsProvider();

    JsonHelper jsonHelper();

    AwsCredentialsProvider awsCredentialsProvider();

    V2IotHelper v2IotHelper();

    V2GreengrassHelper v2GreengrassHelper();

    SslContextHelper sslContextHelper();
}

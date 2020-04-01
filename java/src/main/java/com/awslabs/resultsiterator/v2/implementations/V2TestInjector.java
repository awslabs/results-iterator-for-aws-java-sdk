package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.resultsiterator.v2.V2HelperModule;
import dagger.Component;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
@Component(modules = {V2HelperModule.class})
public interface V2TestInjector {
    S3Client s3Client();

    Provider<S3ClientBuilder> s3ClientBuilder();

    IotClient iotClient();

    GreengrassClient greengrassClient();
}

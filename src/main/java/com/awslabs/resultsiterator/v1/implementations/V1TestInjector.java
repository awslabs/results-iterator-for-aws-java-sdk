package com.awslabs.resultsiterator.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.awslabs.resultsiterator.v1.V1HelperModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {V1HelperModule.class})
public interface V1TestInjector {
    AWSIotClient awsIotClient();

    AmazonS3Client amazonS3Client();
}

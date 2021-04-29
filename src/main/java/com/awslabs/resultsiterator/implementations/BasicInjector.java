package com.awslabs.resultsiterator.implementations;

import com.awslabs.ecr.EcrDockerHelper;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.awslabs.iot.helpers.interfaces.GreengrassV1Helper;
import com.awslabs.iot.helpers.interfaces.GreengrassV2Helper;
import com.awslabs.iot.helpers.interfaces.IotHelper;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;
import com.awslabs.resultsiterator.ResultsIteratorModule;
import com.awslabs.resultsiterator.interfaces.CertificateCredentialsProvider;
import com.awslabs.resultsiterator.interfaces.SslContextHelper;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import com.awslabs.sqs.helpers.interfaces.SqsHelper;
import dagger.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.s3.S3Client;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ResultsIteratorModule.class})
public interface BasicInjector {
    IotClient iotClient();

    GreengrassClient greengrassClient();

    S3Helper s3Helper();

    S3Client s3Client();

    CertificateCredentialsProvider certificateCredentialsProvider();

    AwsCredentialsProvider awsCredentialsProvider();

    IotHelper iotHelper();

    SqsHelper sqsHelper();

    GreengrassV1Helper greengrassV1Helper();

    GreengrassV2Helper greengrassV2Helper();

    SslContextHelper sslContextHelper();

    IamHelper iamHelper();

    IotIdExtractor iotIdExtractor();

    EcrDockerHelper ecrDockerHelper();
}

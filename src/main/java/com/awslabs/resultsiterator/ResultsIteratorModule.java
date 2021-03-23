package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.implementations.BasicLambdaPackagingHelper;
import com.awslabs.general.helpers.implementations.BasicProcessHelper;
import com.awslabs.general.helpers.implementations.IoHelper;
import com.awslabs.general.helpers.implementations.JsonHelper;
import com.awslabs.general.helpers.interfaces.LambdaPackagingHelper;
import com.awslabs.general.helpers.interfaces.ProcessHelper;
import com.awslabs.iam.helpers.implementations.BasicIamHelper;
import com.awslabs.iam.helpers.interfaces.IamHelper;
import com.awslabs.iot.helpers.implementations.*;
import com.awslabs.iot.helpers.interfaces.*;
import com.awslabs.lambda.helpers.implementations.BasicLambdaHelper;
import com.awslabs.lambda.helpers.interfaces.LambdaHelper;
import com.awslabs.resultsiterator.implementations.*;
import com.awslabs.resultsiterator.interfaces.CertificateCredentialsProvider;
import com.awslabs.resultsiterator.interfaces.ReflectionHelper;
import com.awslabs.resultsiterator.interfaces.SdkErrorHandler;
import com.awslabs.resultsiterator.interfaces.SslContextHelper;
import com.awslabs.s3.helpers.implementations.BasicS3Helper;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import com.awslabs.sqs.helpers.implementations.BasicSqsHelper;
import com.awslabs.sqs.helpers.interfaces.SqsHelper;
import dagger.Module;
import dagger.Provides;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.GreengrassClientBuilder;
import software.amazon.awssdk.services.greengrassv2.GreengrassV2Client;
import software.amazon.awssdk.services.greengrassv2.GreengrassV2ClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.IotClientBuilder;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClientBuilder;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

import java.security.Security;

@Module
public class ResultsIteratorModule {
    static {
        // Add BouncyCastle as a security provider in just one place
        Security.addProvider(new BouncyCastleProvider());
    }

    @Provides
    public JsonHelper jsonHelper(JsonHelper basicJsonHelper) {
        return basicJsonHelper;
    }

    @Provides
    public IoHelper ioHelper(IoHelper basicIoHelper) {
        return basicIoHelper;
    }

    @Provides
    public GreengrassV1IdExtractor greengrassIdExtractor(BasicGreengrassV1IdExtractor basicGreengrassIdExtractor) {
        return basicGreengrassIdExtractor;
    }

    @Provides
    public IotIdExtractor iotIdExtractor(BasicIotIdExtractor basicIotIdExtractor) {
        return basicIotIdExtractor;
    }

    @Provides
    public LambdaPackagingHelper lambdaPackagingHelper(BasicLambdaPackagingHelper basicLambdaPackagingHelper) {
        return basicLambdaPackagingHelper;
    }

    @Provides
    public ProcessHelper processHelper(BasicProcessHelper basicProcessHelper) {
        return basicProcessHelper;
    }

    @Provides
    public AwsCredentialsProvider awsCredentialsProvider(CertificateCredentialsProvider certificateCredentialsProvider) {
        return new SafeProvider<>(() -> AwsCredentialsProviderChain.of(certificateCredentialsProvider, DefaultCredentialsProvider.create())).get();
    }

    @Provides
    public AwsRegionProviderChain awsRegionProviderChain() {
        return new SafeProvider<>(DefaultAwsRegionProviderChain::new).get();
    }

    @Provides
    public ApacheHttpClient.Builder apacheHttpClientBuilderProvider() {
        return ApacheHttpClient.builder();
    }

    @Provides
    public CertificateCredentialsProvider certificateCredentialsProvider(BouncyCastleCertificateCredentialsProvider bouncyCastleCertificateCredentialsProvider) {
        return bouncyCastleCertificateCredentialsProvider;
    }

    // Centralized error handling for  SDK errors
    @Provides
    public SdkErrorHandler sdkErrorHandler(BasicSdkErrorHandler basicSdkErrorHandler) {
        return basicSdkErrorHandler;
    }

    // Normal clients that need no special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public StsClientBuilder stsClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return StsClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public StsClient stsClient(StsClientBuilder stsClientBuilder) {
        return new SafeProvider<>(stsClientBuilder::build).get();
    }

    @Provides
    public S3ClientBuilder s3ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return S3Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
        return new SafeProvider<>(s3ClientBuilder::build).get();
    }

    @Provides
    public SqsClientBuilder sqsClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return SqsClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public SqsClient sqsClient(SqsClientBuilder sqsClientBuilder) {
        return new SafeProvider<>(sqsClientBuilder::build).get();
    }

    @Provides
    public IotClientBuilder iotClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IotClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public IotClient iotClient(IotClientBuilder iotClientBuilder) {
        return new SafeProvider<>(iotClientBuilder::build).get();
    }

    @Provides
    public IotDataPlaneClientBuilder iotDataPlaneClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IotDataPlaneClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public IotDataPlaneClient iotDataPlaneClient(IotDataPlaneClientBuilder iotDataPlaneClientBuilder) {
        return new SafeProvider<>(iotDataPlaneClientBuilder::build).get();
    }

    @Provides
    public GreengrassClientBuilder greengrassClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return GreengrassClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public GreengrassClient greengrassClient(GreengrassClientBuilder greengrassClientBuilder) {
        return new SafeProvider<>(greengrassClientBuilder::build).get();
    }

    @Provides
    public GreengrassV2ClientBuilder greengrassV2ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return GreengrassV2Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public GreengrassV2Client greengrassV2Client(GreengrassV2ClientBuilder greengrassV2ClientBuilder) {
        return new SafeProvider<>(greengrassV2ClientBuilder::build).get();
    }

    @Provides
    public LambdaClientBuilder lambdaClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return LambdaClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public LambdaClient lambdaClient(LambdaClientBuilder lambdaClientBuilder) {
        return new SafeProvider<>(lambdaClientBuilder::build).get();
    }

    @Provides
    public Ec2ClientBuilder ec2ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return Ec2Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public Ec2Client ec2Client(Ec2ClientBuilder ec2ClientBuilder) {
        return new SafeProvider<>(ec2ClientBuilder::build).get();
    }

    // Clients that need special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public IamClientBuilder iamClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IamClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider).region(Region.AWS_GLOBAL);
    }

    @Provides
    public IamClient iamClient(IamClientBuilder iamClientBuilder) {
        return new SafeProvider<>(iamClientBuilder::build).get();
    }

    @Provides
    public AwsCredentials awsCredentials(AwsCredentialsProvider awsCredentialsProvider) {
        return new SafeProvider<>(awsCredentialsProvider::resolveCredentials).get();
    }

    @Provides
    public IamHelper iamHelper(BasicIamHelper basicIamHelper) {
        return basicIamHelper;
    }

    @Provides
    public S3Helper s3Helper(BasicS3Helper basicS3Helper) {
        return basicS3Helper;
    }

    @Provides
    public GreengrassV1Helper greengrassHelper(BasicGreengrassV1Helper basicGreengrassHelper) {
        return basicGreengrassHelper;
    }

    @Provides
    public GreengrassV2Helper greengrassV2Helper(BasicGreengrassV2Helper basicGreengrassV2Helper) {
        return basicGreengrassV2Helper;
    }

    @Provides
    public LambdaHelper lambdaHelper(BasicLambdaHelper basicLambdaHelper) {
        return basicLambdaHelper;
    }

    @Provides
    public IotHelper iotHelper(BasicIotHelper basicIotHelper) {
        return basicIotHelper;
    }

    @Provides
    public SqsHelper sqsHelper(BasicSqsHelper basicSqsHelper) {
        return basicSqsHelper;
    }

    @Provides
    public ReflectionHelper reflectionHelper(BasicReflectionHelper basicReflectionHelper) {
        return basicReflectionHelper;
    }

    @Provides
    public SslContextHelper sslContextHelper(BasicSslContextHelper basicSslContextHelper) {
        return basicSslContextHelper;
    }
}

package com.awslabs.resultsiterator.v2;

import com.awslabs.iam.helpers.implementations.BasicV2IamHelper;
import com.awslabs.iam.helpers.interfaces.V2IamHelper;
import com.awslabs.iot.helpers.implementations.BasicV2GreengrassHelper;
import com.awslabs.iot.helpers.implementations.BasicV2IotHelper;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.iot.helpers.interfaces.V2IotHelper;
import com.awslabs.lambda.helpers.implementations.BasicV2LambdaHelper;
import com.awslabs.lambda.helpers.interfaces.V2LambdaHelper;
import com.awslabs.resultsiterator.SharedModule;
import com.awslabs.resultsiterator.implementations.BasicSslContextHelper;
import com.awslabs.resultsiterator.interfaces.SslContextHelper;
import com.awslabs.resultsiterator.v2.implementations.BasicV2ReflectionHelper;
import com.awslabs.resultsiterator.v2.implementations.BasicV2SdkErrorHandler;
import com.awslabs.resultsiterator.v2.implementations.BouncyCastleV2CertificateCredentialsProvider;
import com.awslabs.resultsiterator.v2.implementations.V2SafeProvider;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import com.awslabs.resultsiterator.v2.interfaces.V2ReflectionHelper;
import com.awslabs.resultsiterator.v2.interfaces.V2SdkErrorHandler;
import com.awslabs.s3.helpers.implementations.BasicV2S3Helper;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import com.awslabs.sqs.helpers.implementations.BasicV2SqsHelper;
import com.awslabs.sqs.helpers.interfaces.V2SqsHelper;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.GreengrassClientBuilder;
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

@Module(includes = {SharedModule.class})
public class V2HelperModule {
    @Provides
    public AwsCredentialsProvider awsCredentialsProvider(V2CertificateCredentialsProvider v2CertificateCredentialsProvider) {
        return new V2SafeProvider<>(() -> AwsCredentialsProviderChain.of(v2CertificateCredentialsProvider, DefaultCredentialsProvider.create())).get();
    }

    @Provides
    public AwsRegionProviderChain awsRegionProviderChain() {
        return new V2SafeProvider<>(DefaultAwsRegionProviderChain::new).get();
    }

    @Provides
    public ApacheHttpClient.Builder apacheHttpClientBuilderProvider() {
        return ApacheHttpClient.builder();
    }

    @Provides
    public V2CertificateCredentialsProvider v2CertificateCredentialsProvider(BouncyCastleV2CertificateCredentialsProvider bouncyCastleV2CertificateCredentialsProvider) {
        return bouncyCastleV2CertificateCredentialsProvider;
    }

    // Centralized error handling for V2 SDK errors
    @Provides
    public V2SdkErrorHandler v2SdkErrorHandler(BasicV2SdkErrorHandler basicV2SdkErrorHandler) {
        return basicV2SdkErrorHandler;
    }

    // Normal clients that need no special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public StsClientBuilder stsClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return StsClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public StsClient stsClient(StsClientBuilder stsClientBuilder) {
        return new V2SafeProvider<>(stsClientBuilder::build).get();
    }

    @Provides
    public S3ClientBuilder s3ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return S3Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
        return new V2SafeProvider<>(s3ClientBuilder::build).get();
    }

    @Provides
    public SqsClientBuilder sqsClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return SqsClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public SqsClient sqsClient(SqsClientBuilder sqsClientBuilder) {
        return new V2SafeProvider<>(sqsClientBuilder::build).get();
    }

    @Provides
    public IotClientBuilder iotClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IotClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public IotClient iotClient(IotClientBuilder iotClientBuilder) {
        return new V2SafeProvider<>(iotClientBuilder::build).get();
    }

    @Provides
    public IotDataPlaneClientBuilder iotDataPlaneClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IotDataPlaneClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public IotDataPlaneClient iotDataPlaneClient(IotDataPlaneClientBuilder iotDataPlaneClientBuilder) {
        return new V2SafeProvider<>(iotDataPlaneClientBuilder::build).get();
    }

    @Provides
    public GreengrassClientBuilder greengrassClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return GreengrassClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public GreengrassClient greengrassClient(GreengrassClientBuilder greengrassClientBuilder) {
        return new V2SafeProvider<>(greengrassClientBuilder::build).get();
    }

    @Provides
    public LambdaClientBuilder lambdaClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return LambdaClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public LambdaClient lambdaClient(LambdaClientBuilder lambdaClientBuilder) {
        return new V2SafeProvider<>(lambdaClientBuilder::build).get();
    }

    // Clients that need special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public IamClientBuilder iamClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IamClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider).region(Region.AWS_GLOBAL);
    }

    @Provides
    public IamClient iamClient(IamClientBuilder iamClientBuilder) {
        return new V2SafeProvider<>(iamClientBuilder::build).get();
    }

    @Provides
    public AwsCredentials awsCredentials(AwsCredentialsProvider awsCredentialsProvider) {
        return new V2SafeProvider<>(awsCredentialsProvider::resolveCredentials).get();
    }

    @Provides
    public V2IamHelper iamHelper(BasicV2IamHelper basicV2IamHelper) {
        return basicV2IamHelper;
    }

    @Provides
    public V2S3Helper v2S3Helper(BasicV2S3Helper basicV2S3Helper) {
        return basicV2S3Helper;
    }

    @Provides
    public V2GreengrassHelper v2GreengrassHelper(BasicV2GreengrassHelper basicV2GreengrassHelper) {
        return basicV2GreengrassHelper;
    }

    @Provides
    public V2LambdaHelper v2LambdaHelper(BasicV2LambdaHelper basicV2LambdaHelper) {
        return basicV2LambdaHelper;
    }

    @Provides
    public V2IotHelper v2IotHelper(BasicV2IotHelper basicV2IotHelper) {
        return basicV2IotHelper;
    }

    @Provides
    public V2SqsHelper v2SqsHelper(BasicV2SqsHelper basicV2SqsHelper) {
        return basicV2SqsHelper;
    }

    @Provides
    public V2ReflectionHelper v2ReflectionHelper(BasicV2ReflectionHelper basicV2ReflectionHelper) {
        return basicV2ReflectionHelper;
    }

    @Provides
    public SslContextHelper sslContextHelper(BasicSslContextHelper basicSslContextHelper) {
        return basicSslContextHelper;
    }
}

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
import com.awslabs.resultsiterator.v2.implementations.BasicV2SdkErrorHandler;
import com.awslabs.resultsiterator.v2.implementations.V2SafeProvider;
import com.awslabs.resultsiterator.v2.interfaces.V2SdkErrorHandler;
import com.awslabs.s3.helpers.implementations.BasicV2S3Helper;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.GreengrassClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.IotClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

@Module(includes = {SharedModule.class})
public class V2HelperModule {
    @Provides
    public AwsCredentialsProvider provideAwsCredentialsProvider() {
        return new V2SafeProvider<>(DefaultCredentialsProvider::create).get();
    }

    @Provides
    public AwsRegionProviderChain provideAwsRegionProviderChain() {
        return new V2SafeProvider<>(DefaultAwsRegionProviderChain::new).get();
    }

    // Centralized error handling for V2 SDK errors
    @Provides
    public V2SdkErrorHandler provideV2SdkErrorHandler(BasicV2SdkErrorHandler basicV2SdkErrorHandler) {
        return basicV2SdkErrorHandler;
    }

    // Normal clients that need no special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public StsClientBuilder provideStsClientBuilder(AwsCredentialsProvider awsCredentialsProvider) {
        return StsClient.builder().credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public StsClient provideStsClient(StsClientBuilder stsClientBuilder) {
        return new V2SafeProvider<>(stsClientBuilder::build).get();
    }

    @Provides
    public S3ClientBuilder provideS3ClientBuilder(AwsCredentialsProvider awsCredentialsProvider) {
        return S3Client.builder().credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public S3Client provideS3Client(S3ClientBuilder s3ClientBuilder) {
        return new V2SafeProvider<>(s3ClientBuilder::build).get();
    }

    @Provides
    public IotClientBuilder provideIotClientBuilder(AwsCredentialsProvider awsCredentialsProvider) {
        return IotClient.builder().credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public IotClient provideIotClient(IotClientBuilder iotClientBuilder) {
        return new V2SafeProvider<>(iotClientBuilder::build).get();
    }

    @Provides
    public GreengrassClientBuilder provideGreengrassClientBuilder(AwsCredentialsProvider awsCredentialsProvider) {
        return GreengrassClient.builder().credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    public GreengrassClient provideGreengrassClient(GreengrassClientBuilder greengrassClientBuilder) {
        return new V2SafeProvider<>(greengrassClientBuilder::build).get();
    }

    // Clients that need special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public IamClientBuilder provideIamClientBuilder(AwsCredentialsProvider awsCredentialsProvider) {
        return IamClient.builder().credentialsProvider(awsCredentialsProvider).region(Region.AWS_GLOBAL);
    }

    @Provides
    public IamClient provideIamClient(IamClientBuilder iamClientBuilder) {
        return new V2SafeProvider<>(iamClientBuilder::build).get();
    }

    @Provides
    public AwsCredentials provideAwsCredentials(AwsCredentialsProvider awsCredentialsProvider) {
        return new V2SafeProvider<>(awsCredentialsProvider::resolveCredentials).get();
    }

    @Provides
    public V2IamHelper provideIamHelper(BasicV2IamHelper basicV2IamHelper) {
        return basicV2IamHelper;
    }

    @Provides
    public V2S3Helper provideS3Helper(BasicV2S3Helper basicV2S3Helper) {
        return basicV2S3Helper;
    }

    @Provides
    public V2GreengrassHelper provideGreengrassHelper(BasicV2GreengrassHelper basicV2GreengrassHelper) {
        return basicV2GreengrassHelper;
    }

    @Provides
    public V2LambdaHelper provideLambdaHelper(BasicV2LambdaHelper basicV2LambdaHelper) {
        return basicV2LambdaHelper;
    }

    @Provides
    public V2IotHelper provideIotHelper(BasicV2IotHelper basicV2IotHelper) {
        return basicV2IotHelper;
    }
}

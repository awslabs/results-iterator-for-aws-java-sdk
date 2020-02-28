package com.awslabs.resultsiterator.v2;

import com.awslabs.iam.helpers.implementations.BasicV2IamHelper;
import com.awslabs.iam.helpers.interfaces.V2IamHelper;
import com.awslabs.resultsiterator.SharedModule;
import com.awslabs.resultsiterator.v2.implementations.BasicV2SdkErrorHandler;
import com.awslabs.resultsiterator.v2.implementations.V2SafeProvider;
import com.awslabs.resultsiterator.v2.interfaces.V2SdkErrorHandler;
import com.awslabs.s3.helpers.implementations.BasicV2S3Helper;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import dagger.Module;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

@Module(includes = {SharedModule.class})
public class V2HelperModule {
    // Normal clients that need no special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure

    public StsClient provideStsClient() {
        return new V2SafeProvider<>(StsClient::create).get();
    }

    public S3Client provideS3Client() {
        return new V2SafeProvider<>(S3Client::create).get();
    }

    public AwsRegionProviderChain provideAwsRegionProviderChain() {
        return new V2SafeProvider<>(DefaultAwsRegionProviderChain::new).get();
    }

    // Centralized error handling for V2 SDK errors
    public V2SdkErrorHandler provideV2SdkErrorHandler(BasicV2SdkErrorHandler basicV2SdkErrorHandler) {
        return basicV2SdkErrorHandler;
    }

    // Clients that need special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    public IamClient provideIamClient() {
        return new V2SafeProvider<>(() -> IamClient.builder().region(Region.AWS_GLOBAL).build()).get();
    }

    public AwsCredentials provideAwsCredentials() {
        return new V2SafeProvider<>(() -> DefaultCredentialsProvider.create().resolveCredentials()).get();
    }

    public V2IamHelper provideIamHelper(BasicV2IamHelper basicV2IamHelper) {
        return basicV2IamHelper;
    }

    public V2S3Helper provideS3Helper(BasicV2S3Helper basicV2S3Helper) {
        return basicV2S3Helper;
    }
}

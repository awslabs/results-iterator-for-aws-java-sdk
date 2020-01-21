package com.awslabs.aws.iot.resultsiterator.helpers.v2;

import com.awslabs.aws.iot.resultsiterator.BasicV2SdkErrorHandler;
import com.awslabs.aws.iot.resultsiterator.SafeProvider;
import com.awslabs.aws.iot.resultsiterator.SharedModule;
import com.awslabs.aws.iot.resultsiterator.V2SdkErrorHandler;
import com.awslabs.aws.iot.resultsiterator.helpers.v2.implementations.BasicV2IamHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v2.interfaces.V2IamHelper;
import com.google.inject.AbstractModule;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

public class V2HelperModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new SharedModule());

        // Normal clients that need no special configuration
        // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
        bind(StsClient.class).toProvider(new SafeProvider<>(StsClient::create));
        bind(S3Client.class).toProvider(new SafeProvider<>(S3Client::create));
        bind(AwsRegionProviderChain.class).toProvider(new SafeProvider<>(DefaultAwsRegionProviderChain::new));

        // Centralized error handling for V2 SDK errors
        bind(V2SdkErrorHandler.class).to(BasicV2SdkErrorHandler.class);

        // Clients that need special configuration
        // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
        bind(IamClient.class).toProvider(new SafeProvider<>(() -> IamClient.builder().region(Region.AWS_GLOBAL).build()));
        bind(AwsCredentials.class).toProvider(new SafeProvider<>(() -> DefaultCredentialsProvider.create().resolveCredentials()));

        bind(V2IamHelper.class).to(BasicV2IamHelper.class);
    }
}

package com.awslabs.resultsiterator.v1;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.AwsRegionProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.greengrass.AWSGreengrassClient;
import com.amazonaws.services.greengrass.AWSGreengrassClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.awslabs.ec2.implementations.BasicV1Ec2Helper;
import com.awslabs.ec2.interfaces.V1Ec2Helper;
import com.awslabs.iot.helpers.implementations.*;
import com.awslabs.iot.helpers.interfaces.*;
import com.awslabs.resultsiterator.SharedModule;
import dagger.Module;
import dagger.Provides;

@Module(includes = {SharedModule.class})
public class V1HelperModule {
    @Provides
    public AWSCredentialsProvider provideAWSCredentialsProvider() {
        return DefaultAWSCredentialsProviderChain.getInstance();
    }

    @Provides
    public AwsRegionProviderChain provideAwsRegionProviderChain() {
        return new DefaultAwsRegionProviderChain();
    }

    @Provides
    public AmazonEC2ClientBuilder provideAmazonEC2ClientBuilder(AWSCredentialsProvider awsCredentialsProvider) {
        return AmazonEC2ClientBuilder.standard().withCredentials(awsCredentialsProvider);
    }

    @Provides
    public AmazonEC2Client provideAmazonEC2Client(AmazonEC2ClientBuilder amazonEC2ClientBuilder) {
        return (AmazonEC2Client) amazonEC2ClientBuilder.build();
    }

    @Provides
    public AWSIotClientBuilder provideAwsIotClientBuilder(AWSCredentialsProvider awsCredentialsProvider) {
        return AWSIotClientBuilder.standard().withCredentials(awsCredentialsProvider);
    }

    @Provides
    public AWSIotClient provideAwsIotClient(AWSIotClientBuilder awsIotClientBuilder) {
        return (AWSIotClient) awsIotClientBuilder.build();
    }

    @Provides
    public AmazonS3ClientBuilder provideAmazonS3ClientBuilder(AWSCredentialsProvider awsCredentialsProvider) {
        return AmazonS3ClientBuilder.standard().withCredentials(awsCredentialsProvider);
    }

    @Provides
    public AmazonS3Client provideAmazonS3Client(AmazonS3ClientBuilder amazonS3ClientBuilder) {
        return (AmazonS3Client) amazonS3ClientBuilder.build();
    }

    @Provides
    public AWSIotDataClientBuilder provideAwsIotDataClientBuilder(AWSCredentialsProvider awsCredentialsProvider) {
        return AWSIotDataClientBuilder.standard().withCredentials(awsCredentialsProvider);
    }

    @Provides
    public AWSIotDataClient provideAwsIotDataClient(AWSIotDataClientBuilder awsIotDataClientBuilder) {
        return (AWSIotDataClient) awsIotDataClientBuilder.build();
    }

    @Provides
    public AmazonIdentityManagementClientBuilder provideAmazonIdentityManagementClientBuilder(AWSCredentialsProvider awsCredentialsProvider) {
        return AmazonIdentityManagementClientBuilder.standard().withCredentials(awsCredentialsProvider);
    }

    @Provides
    public AmazonIdentityManagementClient provideAmazonIdentityManagementClient(AmazonIdentityManagementClientBuilder amazonIdentityManagementClientBuilder) {
        return (AmazonIdentityManagementClient) amazonIdentityManagementClientBuilder.build();
    }

    @Provides
    public AWSGreengrassClientBuilder provideAwsGreengrassClientBuilder(AWSCredentialsProvider awsCredentialsProvider) {
        return AWSGreengrassClientBuilder.standard().withCredentials(awsCredentialsProvider);
    }

    @Provides
    public AWSGreengrassClient provideAwsGreengrassClient(AWSGreengrassClientBuilder awsGreengrassClientBuilder) {
        return (AWSGreengrassClient) awsGreengrassClientBuilder.build();
    }

    @Provides
    public V1Ec2Helper provideV1Ec2Helper(BasicV1Ec2Helper basicV1Ec2Helper) {
        return basicV1Ec2Helper;
    }

    @Provides
    public V1GreengrassHelper provideV1GreengrassHelper(BasicV1GreengrassHelper basicV1GreengrassHelper) {
        return basicV1GreengrassHelper;
    }

    @Provides
    public V1CertificateHelper provideV1CertificateHelper(BasicV1CertificateHelper basicV1CertificateHelper) {
        return basicV1CertificateHelper;
    }

    @Provides
    public V1ThingHelper provideV1ThingHelper(BasicV1ThingHelper basicV1ThingHelper) {
        return basicV1ThingHelper;
    }

    @Provides
    public V1PolicyHelper provideV1PolicyHelper(BasicV1PolicyHelper basicV1PolicyHelper) {
        return basicV1PolicyHelper;
    }

    @Provides
    public V1ThingGroupHelper provideV1ThingGroupHelper(BasicV1ThingGroupHelper basicV1ThingGroupHelper) {
        return basicV1ThingGroupHelper;
    }

    @Provides
    public V1IamHelper provideV1IamHelper(BasicV1IamHelper basicV1IamHelper) {
        return basicV1IamHelper;
    }

    @Provides
    public V1RuleHelper provideV1RuleHelper(BasicV1RuleHelper basicV1RuleHelper) {
        return basicV1RuleHelper;
    }
}

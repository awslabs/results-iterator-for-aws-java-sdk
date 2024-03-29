package com.awslabs.resultsiterator;

import com.awslabs.cloudformation.implementations.BasicCloudFormationHelper;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.awslabs.dynamodb.implementations.BasicDynamoDbHelper;
import com.awslabs.dynamodb.interfaces.DynamoDbHelper;
import com.awslabs.general.helpers.implementations.BasicLambdaPackagingHelper;
import com.awslabs.general.helpers.implementations.BasicProcessHelper;
import com.awslabs.general.helpers.implementations.GsonHelper;
import com.awslabs.general.helpers.implementations.IoHelper;
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
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
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
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

import javax.inject.Singleton;
import java.security.Security;

@Module
public class ResultsIteratorModule {
    static {
        // Add BouncyCastle as a security provider in just one place
        Security.addProvider(new BouncyCastleProvider());
    }

    @Provides
    @Singleton
    public GsonHelper gsonHelper(GsonHelper basicGsonHelper) {
        return basicGsonHelper;
    }

    @Provides
    @Singleton
    public IoHelper ioHelper(IoHelper basicIoHelper) {
        return basicIoHelper;
    }

    @Provides
    @Singleton
    public GreengrassV1IdExtractor greengrassIdExtractor(BasicGreengrassV1IdExtractor basicGreengrassIdExtractor) {
        return basicGreengrassIdExtractor;
    }

    @Provides
    @Singleton
    public IotIdExtractor iotIdExtractor(BasicIotIdExtractor basicIotIdExtractor) {
        return basicIotIdExtractor;
    }

    @Provides
    @Singleton
    public LambdaPackagingHelper lambdaPackagingHelper(BasicLambdaPackagingHelper basicLambdaPackagingHelper) {
        return basicLambdaPackagingHelper;
    }

    @Provides
    @Singleton
    public ProcessHelper processHelper(BasicProcessHelper basicProcessHelper) {
        return basicProcessHelper;
    }

    @Provides
    @Singleton
    public AwsCredentialsProvider awsCredentialsProvider(CertificateCredentialsProvider certificateCredentialsProvider) {
        return new SafeProvider<>(() -> AwsCredentialsProviderChain.of(certificateCredentialsProvider, DefaultCredentialsProvider.create())).get();
    }

    @Provides
    @Singleton
    public AwsRegionProviderChain awsRegionProviderChain() {
        return new SafeProvider<>(DefaultAwsRegionProviderChain::new).get();
    }

    @Provides
    @Singleton
    public ApacheHttpClient.Builder apacheHttpClientBuilderProvider() {
        return ApacheHttpClient.builder();
    }

    @Provides
    @Singleton
    public CertificateCredentialsProvider certificateCredentialsProvider(BouncyCastleCertificateCredentialsProvider bouncyCastleCertificateCredentialsProvider) {
        return bouncyCastleCertificateCredentialsProvider;
    }

    // Centralized error handling for SDK errors
    @Provides
    @Singleton
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
    @Singleton
    public StsClient stsClient(StsClientBuilder stsClientBuilder) {
        return new SafeProvider<>(stsClientBuilder::build).get();
    }

    @Provides
    public S3ClientBuilder s3ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return S3Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
        return new SafeProvider<>(s3ClientBuilder::build).get();
    }

    @Provides
    public SqsClientBuilder sqsClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return SqsClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public SqsClient sqsClient(SqsClientBuilder sqsClientBuilder) {
        return new SafeProvider<>(sqsClientBuilder::build).get();
    }

    @Provides
    public IotClientBuilder iotClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IotClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public IotClient iotClient(IotClientBuilder iotClientBuilder) {
        return new SafeProvider<>(iotClientBuilder::build).get();
    }

    @Provides
    public IotDataPlaneClientBuilder iotDataPlaneClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IotDataPlaneClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public IotDataPlaneClient iotDataPlaneClient(IotDataPlaneClientBuilder iotDataPlaneClientBuilder) {
        return new SafeProvider<>(iotDataPlaneClientBuilder::build).get();
    }

    @Provides
    public GreengrassClientBuilder greengrassClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return GreengrassClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public GreengrassClient greengrassClient(GreengrassClientBuilder greengrassClientBuilder) {
        return new SafeProvider<>(greengrassClientBuilder::build).get();
    }

    @Provides
    public GreengrassV2ClientBuilder greengrassV2ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return GreengrassV2Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public GreengrassV2Client greengrassV2Client(GreengrassV2ClientBuilder greengrassV2ClientBuilder) {
        return new SafeProvider<>(greengrassV2ClientBuilder::build).get();
    }

    @Provides
    public LambdaClientBuilder lambdaClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return LambdaClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public LambdaClient lambdaClient(LambdaClientBuilder lambdaClientBuilder) {
        return new SafeProvider<>(lambdaClientBuilder::build).get();
    }

    @Provides
    public Ec2ClientBuilder ec2ClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return Ec2Client.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public Ec2Client ec2Client(Ec2ClientBuilder ec2ClientBuilder) {
        return new SafeProvider<>(ec2ClientBuilder::build).get();
    }

    @Provides
    public CloudFormationClientBuilder cloudFormationClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return CloudFormationClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public CloudFormationClient cloudFormationClient(CloudFormationClientBuilder cloudFormationClientBuilder) {
        return new SafeProvider<>(cloudFormationClientBuilder::build).get();
    }

    @Provides
    public DynamoDbClientBuilder dynamoDbClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return DynamoDbClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider);
    }

    @Provides
    @Singleton
    public DynamoDbClient dynamoDbClient(DynamoDbClientBuilder dynamoDbClientBuilder) {
        return new SafeProvider<>(dynamoDbClientBuilder::build).get();
    }

    // Clients that need special configuration
    // NOTE: Using this pattern allows us to wrap the creation of these clients in some error checking code that can give the user information on what to do in the case of a failure
    @Provides
    public IamClientBuilder iamClientBuilder(AwsCredentialsProvider awsCredentialsProvider, ApacheHttpClient.Builder apacheHttpClientBuilder) {
        return IamClient.builder().httpClientBuilder(apacheHttpClientBuilder).credentialsProvider(awsCredentialsProvider).region(Region.AWS_GLOBAL);
    }

    @Provides
    @Singleton
    public IamClient iamClient(IamClientBuilder iamClientBuilder) {
        return new SafeProvider<>(iamClientBuilder::build).get();
    }

    @Provides
    public AwsCredentials awsCredentials(AwsCredentialsProvider awsCredentialsProvider) {
        return new SafeProvider<>(awsCredentialsProvider::resolveCredentials).get();
    }

    @Provides
    @Singleton
    public IamHelper iamHelper(BasicIamHelper basicIamHelper) {
        return basicIamHelper;
    }

    @Provides
    @Singleton
    public S3Helper s3Helper(BasicS3Helper basicS3Helper) {
        return basicS3Helper;
    }

    @Provides
    @Singleton
    public CloudFormationHelper cloudFormationHelper(BasicCloudFormationHelper basicCloudFormationHelper) {
        return basicCloudFormationHelper;
    }

    @Provides
    @Singleton
    public DynamoDbHelper dynamoDbHelper(BasicDynamoDbHelper basicDynamoDbHelper) {
        return basicDynamoDbHelper;
    }

    @Provides
    @Singleton
    public GreengrassV1Helper greengrassHelper(BasicGreengrassV1Helper basicGreengrassHelper) {
        return basicGreengrassHelper;
    }

    @Provides
    @Singleton
    public GreengrassV2Helper greengrassV2Helper(BasicGreengrassV2Helper basicGreengrassV2Helper) {
        return basicGreengrassV2Helper;
    }

    @Provides
    @Singleton
    public LambdaHelper lambdaHelper(BasicLambdaHelper basicLambdaHelper) {
        return basicLambdaHelper;
    }

    @Provides
    @Singleton
    public IotHelper iotHelper(BasicIotHelper basicIotHelper) {
        return basicIotHelper;
    }

    @Provides
    @Singleton
    public SqsHelper sqsHelper(BasicSqsHelper basicSqsHelper) {
        return basicSqsHelper;
    }

    @Provides
    @Singleton
    public ReflectionHelper reflectionHelper(BasicReflectionHelper basicReflectionHelper) {
        return basicReflectionHelper;
    }

    @Provides
    @Singleton
    public SslContextHelper sslContextHelper(BasicSslContextHelper basicSslContextHelper) {
        return basicSslContextHelper;
    }

    @Provides
    @Singleton
    public S3Utilities s3Utilities(AwsRegionProviderChain awsRegionProviderChain) {
        return S3Utilities.builder().region(awsRegionProviderChain.getRegion()).build();
    }
}

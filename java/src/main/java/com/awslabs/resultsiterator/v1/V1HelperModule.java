package com.awslabs.resultsiterator.v1;

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
    public AmazonEC2Client provideAmazonEC2Client() {
        return (AmazonEC2Client) AmazonEC2ClientBuilder.defaultClient();
    }

    @Provides
    public AWSIotClient provideAwsIotClient() {
        return (AWSIotClient) AWSIotClientBuilder.defaultClient();
    }

    @Provides
    public AWSIotDataClient provideAwsIotDataClient() {
        return (AWSIotDataClient) AWSIotDataClientBuilder.defaultClient();
    }

    @Provides
    public AmazonIdentityManagementClient provideAmazonIdentityManagementClient() {
        return (AmazonIdentityManagementClient) AmazonIdentityManagementClientBuilder.defaultClient();
    }

    @Provides
    public AWSGreengrassClient provideAwsGreengrassClient() {
        return (AWSGreengrassClient) AWSGreengrassClientBuilder.defaultClient();
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

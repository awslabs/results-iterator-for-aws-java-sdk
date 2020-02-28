package com.awslabs.resultsiterator.v1;

import com.amazonaws.services.greengrass.AWSGreengrassClient;
import com.amazonaws.services.greengrass.AWSGreengrassClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.awslabs.iot.helpers.implementations.*;
import com.awslabs.iot.helpers.interfaces.*;
import com.awslabs.resultsiterator.SharedModule;
import com.google.inject.AbstractModule;

public class V1HelperModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new SharedModule());

        // Client providers
        bind(AWSIotClient.class).toProvider(() -> (AWSIotClient) AWSIotClientBuilder.defaultClient());
        bind(AWSIotDataClient.class).toProvider(() -> (AWSIotDataClient) AWSIotDataClientBuilder.defaultClient());
        bind(AmazonIdentityManagementClient.class).toProvider(() -> (AmazonIdentityManagementClient) AmazonIdentityManagementClientBuilder.defaultClient());
        bind(AWSGreengrassClient.class).toProvider(() -> (AWSGreengrassClient) AWSGreengrassClientBuilder.defaultClient());

        bind(V1GreengrassHelper.class).to(BasicV1GreengrassHelper.class);
        bind(V1CertificateHelper.class).to(BasicV1CertificateHelper.class);
        bind(V1ThingHelper.class).to(BasicV1ThingHelper.class);
        bind(V1PolicyHelper.class).to(BasicV1PolicyHelper.class);
        bind(V1ThingGroupHelper.class).to(BasicV1ThingGroupHelper.class);
        bind(V1IamHelper.class).to(BasicV1IamHelper.class);
        bind(V1RuleHelper.class).to(BasicV1RuleHelper.class);
    }
}

package com.awslabs.aws.iot.resultsiterator.helpers.v1;

import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicGreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicIoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicJsonHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.IoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.JsonHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations.*;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.*;
import com.google.inject.AbstractModule;

public class V1HelperModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IoHelper.class).to(BasicIoHelper.class);
        bind(GreengrassIdExtractor.class).to(BasicGreengrassIdExtractor.class);
        bind(V1GreengrassHelper.class).to(BasicV1GreengrassHelper.class);
        bind(V1CertificateHelper.class).to(BasicV1CertificateHelper.class);
        bind(V1ThingHelper.class).to(BasicV1ThingHelper.class);
        bind(V1PolicyHelper.class).to(BasicV1PolicyHelper.class);
        bind(V1ThingGroupHelper.class).to(BasicV1ThingGroupHelper.class);
        bind(V1IamHelper.class).to(BasicV1IamHelper.class);
        bind(JsonHelper.class).to(BasicJsonHelper.class);
    }
}

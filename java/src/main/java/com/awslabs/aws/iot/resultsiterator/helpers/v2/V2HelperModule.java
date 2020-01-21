package com.awslabs.aws.iot.resultsiterator.helpers.v2;

import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicGreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicIoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicJsonHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.IoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.JsonHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v2.implementations.BasicV2IamHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.v2.interfaces.V2IamHelper;
import com.google.inject.AbstractModule;

public class V2HelperModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(JsonHelper.class).to(BasicJsonHelper.class);
        bind(IoHelper.class).to(BasicIoHelper.class);
        bind(GreengrassIdExtractor.class).to(BasicGreengrassIdExtractor.class);

        bind(V2IamHelper.class).to(BasicV2IamHelper.class);
    }
}

package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.implementations.BasicAwsHelper;
import com.awslabs.general.helpers.implementations.BasicIoHelper;
import com.awslabs.general.helpers.implementations.BasicJsonHelper;
import com.awslabs.general.helpers.interfaces.AwsHelper;
import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.helpers.implementations.BasicSharedGreengrassIdExtractor;
import com.awslabs.iot.helpers.interfaces.SharedGreengrassIdExtractor;
import com.google.inject.AbstractModule;

public class SharedModule extends AbstractModule {
    @Override
    public void configure() {
        bind(JsonHelper.class).to(BasicJsonHelper.class);
        bind(IoHelper.class).to(BasicIoHelper.class);
        bind(SharedGreengrassIdExtractor.class).to(BasicSharedGreengrassIdExtractor.class);
        bind(AwsHelper.class).to(BasicAwsHelper.class);
    }
}

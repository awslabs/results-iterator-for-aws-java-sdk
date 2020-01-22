package com.awslabs.aws.iot.resultsiterator;

import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicGreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicIoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.implementations.BasicJsonHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.IoHelper;
import com.awslabs.aws.iot.resultsiterator.helpers.interfaces.JsonHelper;
import com.google.inject.AbstractModule;

public class SharedModule extends AbstractModule {
    @Override
    public void configure() {
        bind(JsonHelper.class).to(BasicJsonHelper.class);
        bind(IoHelper.class).to(BasicIoHelper.class);
        bind(GreengrassIdExtractor.class).to(BasicGreengrassIdExtractor.class);
    }
}

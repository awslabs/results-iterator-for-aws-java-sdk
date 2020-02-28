package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.implementations.BasicAwsHelper;
import com.awslabs.general.helpers.implementations.BasicIoHelper;
import com.awslabs.general.helpers.implementations.BasicJsonHelper;
import com.awslabs.general.helpers.interfaces.AwsHelper;
import com.awslabs.general.helpers.interfaces.IoHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.helpers.implementations.BasicGreengrassIdExtractor;
import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import dagger.Module;
import dagger.Provides;

@Module
public class SharedModule {
    @Provides
    public JsonHelper provideJsonHelper(BasicJsonHelper basicJsonHelper) {
        return basicJsonHelper;
    }

    @Provides
    public IoHelper provideIoHelper(BasicIoHelper basicIoHelper) {
        return basicIoHelper;
    }

    @Provides
    public GreengrassIdExtractor provideGreengrassIdExtractor(BasicGreengrassIdExtractor basicSharedGreengrassIdExtractor) {
        return basicSharedGreengrassIdExtractor;
    }

    @Provides
    public AwsHelper provideAwsHelper(BasicAwsHelper basicAwsHelper) {
        return basicAwsHelper;
    }
}

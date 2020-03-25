package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.implementations.*;
import com.awslabs.general.helpers.interfaces.*;
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

    @Provides
    public LambdaPackagingHelper provideLambdaPackagingHelper(BasicLambdaPackagingHelper basicLambdaPackagingHelper) {
        return basicLambdaPackagingHelper;
    }

    @Provides
    public ProcessHelper provideProcessHelper(BasicProcessHelper basicProcessHelper) {
        return basicProcessHelper;
    }
}

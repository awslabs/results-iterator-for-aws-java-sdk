package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.implementations.*;
import com.awslabs.general.helpers.interfaces.*;
import com.awslabs.iot.helpers.implementations.BasicGreengrassIdExtractor;
import com.awslabs.iot.helpers.implementations.BasicIotIdExtractor;
import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;
import dagger.Module;
import dagger.Provides;

@Module
public class SharedModule {
    @Provides
    public JsonHelper jsonHelper(JsonHelper basicJsonHelper) {
        return basicJsonHelper;
    }

    @Provides
    public IoHelper ioHelper(BasicIoHelper basicIoHelper) {
        return basicIoHelper;
    }

    @Provides
    public GreengrassIdExtractor greengrassIdExtractor(BasicGreengrassIdExtractor basicGreengrassIdExtractor) {
        return basicGreengrassIdExtractor;
    }

    @Provides
    public IotIdExtractor iotIdExtractor(BasicIotIdExtractor basicIotIdExtractor) {
        return basicIotIdExtractor;
    }

    @Provides
    public AwsHelper awsHelper(BasicAwsHelper basicAwsHelper) {
        return basicAwsHelper;
    }

    @Provides
    public LambdaPackagingHelper lambdaPackagingHelper(BasicLambdaPackagingHelper basicLambdaPackagingHelper) {
        return basicLambdaPackagingHelper;
    }

    @Provides
    public ProcessHelper processHelper(BasicProcessHelper basicProcessHelper) {
        return basicProcessHelper;
    }
}

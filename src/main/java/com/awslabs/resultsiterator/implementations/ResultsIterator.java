package com.awslabs.resultsiterator.implementations;

import com.awslabs.resultsiterator.interfaces.ReflectionHelper;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.SdkClient;

public class ResultsIterator<T> extends ResultsIteratorAbstract<T> {
    public ResultsIterator(ReflectionHelper reflectionHelper, SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        super(reflectionHelper, sdkClient, awsRequestClass);
    }

    public ResultsIterator(ReflectionHelper reflectionHelper, SdkClient sdkClient, AwsRequest originalAwsRequest) {
        super(reflectionHelper, sdkClient, originalAwsRequest);
    }

    public ResultsIterator(SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        super(sdkClient, awsRequestClass);
    }

    public ResultsIterator(SdkClient sdkClient, AwsRequest originalAwsRequest) {
        super(sdkClient, originalAwsRequest);
    }
}

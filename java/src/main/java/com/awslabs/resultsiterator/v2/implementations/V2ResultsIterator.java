package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.resultsiterator.v2.interfaces.V2ReflectionHelper;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.SdkClient;

public class V2ResultsIterator<T> extends V2ResultsIteratorAbstract<T> {
    public V2ResultsIterator(V2ReflectionHelper v2ReflectionHelper, SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        super(v2ReflectionHelper, sdkClient, awsRequestClass);
    }

    public V2ResultsIterator(V2ReflectionHelper v2ReflectionHelper, SdkClient sdkClient, AwsRequest originalAwsRequest) {
        super(v2ReflectionHelper, sdkClient, originalAwsRequest);
    }

    public V2ResultsIterator(SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        super(sdkClient, awsRequestClass);
    }

    public V2ResultsIterator(SdkClient sdkClient, AwsRequest originalAwsRequest) {
        super(sdkClient, originalAwsRequest);
    }
}

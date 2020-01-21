package com.awslabs.aws.iot.resultsiterator;

import software.amazon.awssdk.core.exception.SdkClientException;

public interface SdkErrorHandler {
    Void handleSdkError(SdkClientException e);
}

package com.awslabs.aws.iot.resultsiterator;

import software.amazon.awssdk.core.exception.SdkClientException;

public interface V2SdkErrorHandler {
    Void handleSdkError(SdkClientException e);
}

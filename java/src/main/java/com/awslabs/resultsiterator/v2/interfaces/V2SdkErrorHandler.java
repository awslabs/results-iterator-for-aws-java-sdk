package com.awslabs.resultsiterator.v2.interfaces;

import software.amazon.awssdk.core.exception.SdkClientException;

public interface V2SdkErrorHandler {
    Void handleSdkError(SdkClientException e);
}

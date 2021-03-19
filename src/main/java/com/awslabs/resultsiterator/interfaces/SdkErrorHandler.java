package com.awslabs.resultsiterator.interfaces;

import software.amazon.awssdk.core.exception.SdkClientException;

public interface SdkErrorHandler {
    Void handleSdkError(SdkClientException e);
}

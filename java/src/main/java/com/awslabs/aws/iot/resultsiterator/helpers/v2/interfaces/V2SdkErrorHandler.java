package com.awslabs.aws.iot.resultsiterator.helpers.v2.interfaces;

import software.amazon.awssdk.core.exception.SdkClientException;

public interface V2SdkErrorHandler {
    Void handleSdkError(SdkClientException e);
}

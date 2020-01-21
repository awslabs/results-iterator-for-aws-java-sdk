package com.awslabs.aws.iot.resultsiterator;

import io.vavr.control.Try;
import software.amazon.awssdk.core.exception.SdkClientException;

import javax.inject.Provider;
import java.util.concurrent.Callable;

public class SafeProvider<T> implements Provider<T> {
    private final Callable<T> callable;

    public SafeProvider(Callable<T> callable) {
        this.callable = callable;
    }

    public T get() {
        V2SdkErrorHandler v2SdkErrorHandler = new BasicV2SdkErrorHandler();

        return Try.of(callable::call)
                .recover(SdkClientException.class, throwable -> (T) v2SdkErrorHandler.handleSdkError(throwable))
                .get();
    }
}

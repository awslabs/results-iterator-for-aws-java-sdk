package com.awslabs.aws.iot.resultsiterator.helpers.v2.implementations;

import com.awslabs.aws.iot.resultsiterator.helpers.v2.interfaces.V2SdkErrorHandler;
import io.vavr.control.Try;
import software.amazon.awssdk.core.exception.SdkClientException;

import javax.inject.Provider;
import java.util.concurrent.Callable;

public class V2SafeProvider<T> implements Provider<T> {
    private final Callable<T> callable;

    public V2SafeProvider(Callable<T> callable) {
        this.callable = callable;
    }

    public T get() {
        V2SdkErrorHandler v2SdkErrorHandler = new BasicV2SdkErrorHandler();

        return Try.of(callable::call)
                .recover(SdkClientException.class, throwable -> (T) v2SdkErrorHandler.handleSdkError(throwable))
                .get();
    }
}

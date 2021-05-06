package com.awslabs.resultsiterator.implementations;

import com.awslabs.resultsiterator.interfaces.ResultsIterator;
import com.awslabs.resultsiterator.interfaces.ReflectionHelper;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

public abstract class ResultsIteratorAbstract<T> implements ResultsIterator<T> {
    private final Logger log = LoggerFactory.getLogger(ResultsIteratorAbstract.class);
    private final SdkClient sdkClient;
    private final Class<? extends AwsRequest> awsRequestClass;
    private final List<String> primaryTokenMethodNames = List.of("nextToken", "nextMarker");
    private final List<String> secondaryTokenMethodNames = List.of("marker");
    private final AwsRequest originalAwsRequest;
    private final ReflectionHelper reflectionHelper;
    private Option<? extends Class<? extends AwsResponse>> responseClassOption = Option.none();
    private AwsResponse awsResponse;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Option<Method> clientMethodReturningResult = null;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Option<Method> clientMethodReturningListT = null;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Option<Method> clientGetMethodReturningString = null;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Option<Method> clientSetMethodAcceptingString = null;

    public ResultsIteratorAbstract(ReflectionHelper reflectionHelper, SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        this.reflectionHelper = reflectionHelper;
        this.sdkClient = sdkClient;
        this.awsRequestClass = awsRequestClass;
        this.originalAwsRequest = null;
    }

    public ResultsIteratorAbstract(ReflectionHelper reflectionHelper, SdkClient sdkClient, AwsRequest originalAwsRequest) {
        this.reflectionHelper = reflectionHelper;
        this.sdkClient = sdkClient;
        this.awsRequestClass = originalAwsRequest.getClass();
        this.originalAwsRequest = originalAwsRequest;
    }

    public ResultsIteratorAbstract(SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        this.reflectionHelper = new BasicReflectionHelper();
        this.sdkClient = sdkClient;
        this.awsRequestClass = awsRequestClass;
        this.originalAwsRequest = null;
    }

    public ResultsIteratorAbstract(SdkClient sdkClient, AwsRequest originalAwsRequest) {
        this.reflectionHelper = new BasicReflectionHelper();
        this.sdkClient = sdkClient;
        this.awsRequestClass = originalAwsRequest.getClass();
        this.originalAwsRequest = originalAwsRequest;
    }

    @Override
    public Stream<T> stream() {
        Iterator<T> iterator = new Iterator<T>() {
            List<T> output = List.empty();
            boolean started = false;
            String nextToken = null;
            AwsRequest request;

            private void performRequest() {
                if (!started) {
                    // First time around configure the request
                    request = configureRequest();

                    // The setup is complete, don't do it again
                    started = true;
                }

                awsResponse = queryNextResults(request);

                output = output.appendAll(getResultData());

                nextToken = getNextToken();

                if (nextToken == null) {
                    return;
                }

                request = setNextToken(request, nextToken);
            }

            @Override
            public boolean hasNext() {
                if (!started) {
                    // We haven't started, attempt a request
                    performRequest();
                }

                while ((output.size() == 0) && (nextToken != null)) {
                    // Output array is empty but the next token is not null, attempt a request
                    performRequest();
                }

                // Next token is NULL, return whether or not the output array is empty
                return output.size() != 0;
            }

            @Override
            public T next() {
                T nextValue = output.get();
                output = output.removeAt(0);

                return nextValue;
            }
        };

        // This stream does not have a known size, does not contain NULL elements, and can not be run in parallel
        return Stream.ofAll(iterator);
    }

    private AwsRequest configureRequest() {
        if (originalAwsRequest != null) {
            // Use the existing request
            return originalAwsRequest.toBuilder().build();
        }

        return reflectionHelper.getNewRequest(awsRequestClass);
    }

    private AwsResponse queryNextResults(AwsRequest request) {
        if (clientMethodReturningResult == null) {
            // Look for a public method in the client (AWSIot, etc) that takes a AwsRequest and returns a V.  If zero or more than one exists, fail.
            clientMethodReturningResult = reflectionHelper.getMethodWithParameterAndReturnType(sdkClient.getClass(), awsRequestClass, getResponseClass());
        }

        if (clientMethodReturningResult.isEmpty()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected response type, this should never happen.");
        }

        try {
            // This is necessary because these methods are not accessible by default
            clientMethodReturningResult.get().setAccessible(true);
            return (AwsResponse) clientMethodReturningResult.get().invoke(sdkClient, request);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SdkClientException) {
                SdkClientException sdkClientException = (SdkClientException) e.getTargetException();

                if (sdkClientException.getMessage().contains("Unable to execute HTTP request")) {
                    log.error("Unable to connect to the API.  Do you have an Internet connection?");
                    return null;
                }
            }

            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private Class<? extends AwsResponse> getResponseClass() {
        synchronized (this) {
            if (responseClassOption.isEmpty()) {
                String requestClassName = awsRequestClass.getName();
                String responseClassName = requestClassName.replaceAll("Request$", "Response");

                try {
                    responseClassOption = Option.of((Class<? extends AwsResponse>) Class.forName(responseClassName));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException(e);
                }
            }
        }

        return responseClassOption.get();
    }

    private List<T> getResultData() {
        if (clientMethodReturningListT == null) {
            // Look for a public method that takes no arguments and returns a java.util.List<T>.  If zero or more than one exists, fail.
            // From: https://stackoverflow.com/a/1901275/796579
            Class<T> returnClass = Try.of(() -> (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0])
                    .orElse(Try.of(() -> (Class<T>) getClass().getGenericSuperclass()))
                    .getOrElse((Class<T>) java.util.List.class);
            clientMethodReturningListT = reflectionHelper.getMethodWithParameterAndReturnType(getResponseClass(), null, returnClass);
        }

        if (clientMethodReturningListT.isEmpty()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected list type, this should never happen.");
        }

        try {
            return List.ofAll((java.util.List<T>) clientMethodReturningListT.get().invoke(awsResponse));
        } catch (IllegalAccessException |
                InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }

    }

    private String getNextToken() {
        if (clientGetMethodReturningString == null) {
            // Look for a public method that takes no arguments and returns a string that matches our list of expected names.  If zero or more than one exists, fail.
            clientGetMethodReturningString = reflectionHelper.getMethodWithParameterReturnTypeAndNames(getResponseClass(), null, String.class, primaryTokenMethodNames);

            if (clientGetMethodReturningString.isEmpty()) {
                // Only look for the secondary method if the primary methods aren't there
                reflectionHelper.getMethodWithParameterReturnTypeAndNames(getResponseClass(), null, String.class, secondaryTokenMethodNames);
            }
        }

        if (clientGetMethodReturningString.isEmpty()) {
            // Some methods like S3's listBuckets do not have pagination
            return null;
        }

        try {
            return (String) clientGetMethodReturningString.get().invoke(awsResponse);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private AwsRequest setNextToken(AwsRequest request, String nextToken) {
        if (clientGetMethodReturningString.isEmpty()) {
            throw new UnsupportedOperationException("Trying to set the next token on a method that does not support pagination, this should never happen.");
        }

        if (clientSetMethodAcceptingString == null) {
            // Look for a public method that takes a string and returns a builder class that matches our list of expected names.  If zero or more than one exists, fail.
            Class<? extends AwsRequest.Builder> builderClass = request.toBuilder().getClass();
            clientSetMethodAcceptingString = reflectionHelper.getMethodWithParameterReturnTypeAndNames(builderClass, String.class, builderClass, primaryTokenMethodNames);

            if (clientSetMethodAcceptingString.isEmpty()) {
                // Only look for these methods if the first search fails
                clientSetMethodAcceptingString = reflectionHelper.getMethodWithParameterReturnTypeAndNames(builderClass, String.class, builderClass, secondaryTokenMethodNames);
            }
        }

        if (clientSetMethodAcceptingString.isEmpty()) {
            throw new UnsupportedOperationException("Failed to find the set next token method, this should never happen.");
        }

        try {
            AwsRequest.Builder builder = request.toBuilder();
            clientSetMethodAcceptingString.get().setAccessible(true);
            clientSetMethodAcceptingString.get().invoke(builder, nextToken);
            return builder.build();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }
}

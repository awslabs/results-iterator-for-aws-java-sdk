package com.awslabs.resultsiterator.implementations;

import com.awslabs.resultsiterator.interfaces.ReflectionHelper;
import com.awslabs.resultsiterator.interfaces.ResultsIteratorInterface;
import io.vavr.Lazy;
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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;

public abstract class ResultsIteratorAbstract<T> implements ResultsIteratorInterface<T> {
    private final Logger log = LoggerFactory.getLogger(ResultsIteratorAbstract.class);
    private final SdkClient sdkClient;
    private final Class<? extends AwsRequest> awsRequestClass;
    private final List<String> primaryTokenMethodNames = List.of("nextToken", "nextMarker");
    private final List<String> secondaryTokenMethodNames = List.of("marker");
    private final Option<AwsRequest> originalAwsRequest;
    private final ReflectionHelper reflectionHelper;
    private Option<? extends Class<? extends AwsResponse>> responseClassOption = Option.none();
    private AwsResponse awsResponse;
    private Lazy<Option<Method>> lazyClientMethodReturningResult = Lazy.of(Option::none);
    private Lazy<Option<Method>> lazyClientMethodReturningListT = Lazy.of(Option::none);
    private Lazy<Option<Method>> lazyClientGetMethodReturningString = Lazy.of(Option::none);
    private Lazy<Option<Method>> lazyClientSetMethodAcceptingString = Lazy.of(Option::none);

    public ResultsIteratorAbstract(ReflectionHelper reflectionHelper, SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        this.reflectionHelper = reflectionHelper;
        this.sdkClient = sdkClient;
        this.awsRequestClass = awsRequestClass;
        this.originalAwsRequest = Option.none();
    }

    public ResultsIteratorAbstract(ReflectionHelper reflectionHelper, SdkClient sdkClient, AwsRequest originalAwsRequest) {
        this.reflectionHelper = reflectionHelper;
        this.sdkClient = sdkClient;
        this.awsRequestClass = originalAwsRequest.getClass();
        this.originalAwsRequest = Option.of(originalAwsRequest);
    }

    public ResultsIteratorAbstract(SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        this.reflectionHelper = new BasicReflectionHelper();
        this.sdkClient = sdkClient;
        this.awsRequestClass = awsRequestClass;
        this.originalAwsRequest = Option.none();
    }

    public ResultsIteratorAbstract(SdkClient sdkClient, AwsRequest originalAwsRequest) {
        this.reflectionHelper = new BasicReflectionHelper();
        this.sdkClient = sdkClient;
        this.awsRequestClass = originalAwsRequest.getClass();
        this.originalAwsRequest = Option.of(originalAwsRequest);
    }

    @Override
    public Stream<T> stream() {
        Iterator<T> iterator = new Iterator<T>() {
            List<T> output = List.empty();
            boolean started = false;
            Option<String> nextToken = Option.none();
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

                if (nextToken.isEmpty()) {
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

                while ((output.size() == 0) && (nextToken.isDefined())) {
                    // Output array is empty but the next token is defined, attempt a request
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
        return originalAwsRequest
                // Use the existing request if there is one
                .map(request -> request.toBuilder().build())
                // Otherwise create a new one
                .getOrElse(() -> reflectionHelper.getNewRequest(awsRequestClass));
    }

    private AwsResponse queryNextResults(AwsRequest request) {
        // Only do this check the first time we run
        if (!lazyClientMethodReturningResult.isEvaluated()) {
            // Look for a public method in the client (AWSIot, etc) that takes a AwsRequest and returns a V.  If zero or more than one exists, fail.
            lazyClientMethodReturningResult = Lazy.of(() -> reflectionHelper.getMethodWithParameterAndReturnType(sdkClient.getClass(), Option.of(awsRequestClass), getResponseClass()));
        }

        if (lazyClientMethodReturningResult.get().isEmpty()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected response type, this should never happen.");
        }

        Method clientMethodReturningResult = lazyClientMethodReturningResult.get().get();

        // This is necessary because these methods are not accessible by default
        Try.run(() -> clientMethodReturningResult.setAccessible(true)).get();

        return Try.of(() -> (AwsResponse) clientMethodReturningResult.invoke(sdkClient, request))
                .mapFailure(Case($(instanceOf(InvocationTargetException.class)), InvocationTargetException::getTargetException))
                .onFailure(SdkClientException.class, this::offerAssistanceForSdkClientExceptionsIfPossible)
                .get();
    }

    private void offerAssistanceForSdkClientExceptionsIfPossible(SdkClientException sdkClientException) {
        if (sdkClientException.getMessage().contains("Unable to execute HTTP request")) {
            log.error("Unable to connect to the API.  Do you have an Internet connection?");
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
        // Only do this check the first time we run
        if (!lazyClientMethodReturningListT.isEvaluated()) {
            // Look for a public method that takes no arguments and returns a java.util.List<T>.  If zero or more than one exists, fail.
            // From: https://stackoverflow.com/a/1901275/796579
            Class<T> returnClass = Try.of(() -> (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0])
                    .orElse(Try.of(() -> (Class<T>) getClass().getGenericSuperclass()))
                    .getOrElse((Class<T>) java.util.List.class);
            lazyClientMethodReturningListT = Lazy.of(() -> reflectionHelper.getMethodWithParameterAndReturnType(getResponseClass(), Option.none(), returnClass));
        }

        if (lazyClientMethodReturningListT.get().isEmpty()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected list type, this should never happen.");
        }

        Method clientMethodReturningListT = lazyClientMethodReturningListT.get().get();

        try {
            return List.ofAll((java.util.List<T>) clientMethodReturningListT.invoke(awsResponse));
        } catch (IllegalAccessException |
                InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }

    }

    private Option<String> getNextToken() {
        // Only do this check the first time we run
        if (!lazyClientGetMethodReturningString.isEvaluated()) {
            // Look for a public method that takes no arguments and returns a string that matches our list of expected names.  If zero or more than one exists, fail.
            lazyClientGetMethodReturningString = Lazy.of(() -> reflectionHelper.getMethodWithParameterReturnTypeAndNames(getResponseClass(), Option.none(), String.class, primaryTokenMethodNames));

            if (lazyClientGetMethodReturningString.get().isEmpty()) {
                // Only look for the secondary method if the primary methods aren't there
                lazyClientGetMethodReturningString = Lazy.of(() -> reflectionHelper.getMethodWithParameterReturnTypeAndNames(getResponseClass(), Option.none(), String.class, secondaryTokenMethodNames));
            }
        }

        if (lazyClientGetMethodReturningString.get().isEmpty()) {
            // Some methods like S3's listBuckets do not have pagination
            return Option.none();
        }

        Method clientGetMethodReturningString = lazyClientGetMethodReturningString.get().get();

        String result = Try.of(() -> (String) clientGetMethodReturningString.invoke(awsResponse))
                // Call get to explicitly throw exceptions, don't use toOption() here since it will hide exceptions
                .get();

        return Option.of(result);
    }

    private AwsRequest setNextToken(AwsRequest request, Option<String> nextToken) {
        if (lazyClientGetMethodReturningString.isEmpty()) {
            throw new UnsupportedOperationException("Trying to set the next token on a method that does not support pagination, this should never happen.");
        }

        // Only do this check the first time we run
        if (!lazyClientSetMethodAcceptingString.isEvaluated()) {
            // Look for a public method that takes a string and returns a builder class that matches our list of expected names.  If zero or more than one exists, fail.
            Class<? extends AwsRequest.Builder> builderClass = request.toBuilder().getClass();
            lazyClientSetMethodAcceptingString = Lazy.of(() -> reflectionHelper.getMethodWithParameterReturnTypeAndNames(builderClass, Option.of(String.class), builderClass, primaryTokenMethodNames));

            if (lazyClientSetMethodAcceptingString.get().isEmpty()) {
                // Only look for these methods if the first search fails
                lazyClientSetMethodAcceptingString = Lazy.of(() -> reflectionHelper.getMethodWithParameterReturnTypeAndNames(builderClass, Option.of(String.class), builderClass, secondaryTokenMethodNames));
            }
        }

        if (lazyClientSetMethodAcceptingString.get().isEmpty()) {
            throw new UnsupportedOperationException("Failed to find the set next token method, this should never happen.");
        }

        Method clientSetMethodAcceptingString = lazyClientSetMethodAcceptingString.get().get();

        try {
            AwsRequest.Builder builder = request.toBuilder();
            clientSetMethodAcceptingString.setAccessible(true);
            clientSetMethodAcceptingString.invoke(builder, nextToken.getOrNull());
            return builder.build();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }
}

package com.awslabs.aws.iot.resultsiterator.helpers.v2;

import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class V2ResultsIterator<T> {
    private final Logger log = LoggerFactory.getLogger(V2ResultsIterator.class);
    private final SdkClient sdkClient;
    private final Class<? extends AwsRequest> awsRequestClass;
    private final List<String> tokenMethodNames = new ArrayList<>(Arrays.asList("nextToken", "nextMarker"));
    private final List<String> methodsToIgnore = new ArrayList<>(Arrays.asList("sdkFields", "commonPrefixes"));
    private final AwsRequest originalAwsRequest;
    private Optional<? extends Class<? extends AwsResponse>> optionalResponseClass = Optional.empty();
    private AwsResponse awsResponse;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Optional<Method> clientMethodReturningResult = null;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Optional<Method> clientMethodReturningListT = null;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Optional<Method> clientGetMethodReturningString = null;
    // NOTE: This is initialized to null so we can determine if we have tried to initialize it already
    private Optional<Method> clientSetMethodAcceptingString = null;

    public V2ResultsIterator(SdkClient sdkClient, Class<? extends AwsRequest> awsRequestClass) {
        this.sdkClient = sdkClient;
        this.awsRequestClass = awsRequestClass;
        this.originalAwsRequest = null;
    }

    public V2ResultsIterator(SdkClient sdkClient, AwsRequest originalAwsRequest) {
        this.sdkClient = sdkClient;
        this.awsRequestClass = originalAwsRequest.getClass();
        this.originalAwsRequest = originalAwsRequest;
    }

    public Stream<T> resultStream() {
        Iterator<T> iterator = new Iterator<T>() {
            List<T> output = new ArrayList<>();
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

                output.addAll(getResultData());

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

                if (output.size() != 0) {
                    // Output array is not empty, there is at least one more element
                    return true;
                }

                // Output array is empty and the next token is NULL
                return false;
            }

            @Override
            public T next() {
                return output.remove(0);
            }
        };

        // This stream does not have a known size, does not contain NULL elements, and can not be run in parallel
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL), false);
    }

    private AwsRequest configureRequest() {
        if (originalAwsRequest != null) {
            return originalAwsRequest.toBuilder().build();
        }

        try {
            // Get a new request object.  If this can't be done with a default constructor it will fail.
            Method method = awsRequestClass.getMethod("builder", AwsRequest.Builder.class);
            return (AwsRequest) method.invoke(null);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private AwsResponse queryNextResults(AwsRequest request) {
        if (clientMethodReturningResult == null) {
            // Look for a public method in the client (AWSIot, etc) that takes a AwsRequest and returns a V.  If zero or more than one exists, fail.
            clientMethodReturningResult = getMethodWithParameterAndReturnType(sdkClient.getClass(), awsRequestClass, getResponseClass());
        }

        if (!clientMethodReturningResult.isPresent()) {
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
            if (!optionalResponseClass.isPresent()) {
                String requestClassName = awsRequestClass.getName();
                String responseClassName = requestClassName.replaceAll("Request$", "Response");

                try {
                    optionalResponseClass = Optional.of((Class<? extends AwsResponse>) Class.forName(responseClassName));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException(e);
                }
            }
        }

        return optionalResponseClass.get();
    }

    private List<T> getResultData() {
        if (clientMethodReturningListT == null) {
            // Look for a public method that takes no arguments and returns a List<T>.  If zero or more than one exists, fail.
            clientMethodReturningListT = getMethodWithParameterAndReturnType(getResponseClass(), null, new TypeToken<List<T>>(getClass()) {
            }.getRawType());
        }

        if (!clientMethodReturningListT.isPresent()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected list type, this should never happen.");
        }

        try {
            return (List<T>) clientMethodReturningListT.get().invoke(awsResponse);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private String getNextToken() {
        if (clientGetMethodReturningString == null) {
            // Look for a public method that takes no arguments and returns a string that matches our list of expected names.  If zero or more than one exists, fail.
            clientGetMethodReturningString = getMethodWithParameterReturnTypeAndNames(getResponseClass(), null, String.class, tokenMethodNames);
        }

        if (!clientGetMethodReturningString.isPresent()) {
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
        if (!clientGetMethodReturningString.isPresent()) {
            throw new UnsupportedOperationException("Trying to set the next token on a method that does not support pagination, this should never happen.");
        }

        if (clientSetMethodAcceptingString == null) {
            // Look for a public method that takes a string and returns a builder class that matches our list of expected names.  If zero or more than one exists, fail.
            Class<? extends AwsRequest.Builder> builderClass = request.toBuilder().getClass();
            clientSetMethodAcceptingString = getMethodWithParameterReturnTypeAndNames(builderClass, String.class, builderClass, tokenMethodNames);
        }

        if (!clientSetMethodAcceptingString.isPresent()) {
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

    private Optional<Method> getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType) {
        return getMethodWithParameterReturnTypeAndName(clazz, parameter, returnType, null);
    }

    private Optional<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name) {
        List<String> names = new ArrayList<>();

        if (name != null) {
            names.add(name);
        }

        return getMethodWithParameterReturnTypeAndNames(clazz, parameter, returnType, names);
    }

    private Optional<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names) {
        Method returnMethod = null;

        for (Method method : clazz.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                // Not public, ignore
                continue;
            }

            String methodName = method.getName();

            if (methodsToIgnore.contains(methodName)) {
                // Always ignore these methods
                continue;
            }

            if ((names.size() > 0) && (!names.contains(method.getName()))) {
                // Not an expected name, ignore
                continue;
            }

            if (parameter != null) {
                if (method.getParameterCount() != 1) {
                    // Not the right number of parameters, ignore
                    continue;
                }

                if (!method.getParameterTypes()[0].equals(parameter)) {
                    // Not the right parameter type, ignore
                    continue;
                }
            }

            if (!method.getReturnType().isAssignableFrom(returnType)) {
                // Not the right return type, ignore
                continue;
            }

            if (returnMethod != null) {
                // More than one match found, fail
                throw new UnsupportedOperationException("Multiple methods found, cannot continue");
            }

            returnMethod = method;
        }

        return Optional.ofNullable(returnMethod);
    }
}

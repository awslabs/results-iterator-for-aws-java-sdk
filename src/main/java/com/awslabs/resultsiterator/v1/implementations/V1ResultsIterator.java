package com.awslabs.resultsiterator.v1.implementations;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.SdkClientException;
import com.awslabs.resultsiterator.interfaces.ResultsIterator;
import com.google.common.reflect.TypeToken;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class V1ResultsIterator<T> implements ResultsIterator<T> {
    private final Logger log = LoggerFactory.getLogger(V1ResultsIterator.class);
    private final AmazonWebServiceClient amazonWebServiceClient;
    private final Class<? extends AmazonWebServiceRequest> requestClass;
    private final List<String> getTokenMethodNames = List.of("getNextToken", "getMarker", "getNextMarker");
    private final List<String> setTokenMethodNames = List.of("setNextToken", "setMarker", "setNextMarker");
    private final AmazonWebServiceRequest originalRequest;
    private Option<Class<? extends AmazonWebServiceResult>> optionalResultClass = Option.none();
    private AmazonWebServiceResult result;
    private Method clientMethodReturningResult;
    private Method clientMethodReturningListT;
    private Method clientGetMethodReturningString;
    private Method clientSetMethodAcceptingString;

    public V1ResultsIterator(AmazonWebServiceClient amazonWebServiceClient, Class<? extends AmazonWebServiceRequest> requestClass) {
        this.amazonWebServiceClient = amazonWebServiceClient;
        this.requestClass = requestClass;
        this.originalRequest = null;
    }

    public V1ResultsIterator(AmazonWebServiceClient amazonWebServiceClient, AmazonWebServiceRequest originalRequest) {
        this.amazonWebServiceClient = amazonWebServiceClient;
        this.requestClass = originalRequest.getClass();
        this.originalRequest = originalRequest;
    }

    @Override
    public Stream<T> stream() {
        Iterator<T> iterator = new Iterator<T>() {
            List<T> output = List.empty();
            boolean started = false;
            String nextToken = null;
            AmazonWebServiceRequest request;

            private void performRequest() {
                if (!started) {
                    // First time around configure the request
                    request = configureRequest();

                    // The setup is complete, don't do it again
                    started = true;
                }

                result = queryNextResults(request);

                output = output.appendAll(getResultData());

                nextToken = getNextToken();

                if (nextToken == null) {
                    return;
                }

                setNextToken(request, nextToken);
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
                T returnValue = output.get();

                output = output.removeAt(0);

                return returnValue;
            }
        };

        // This stream does not have a known size, does not contain NULL elements, and can not be run in parallel
        return Stream.ofAll(iterator);
    }

    private AmazonWebServiceRequest configureRequest() {
        if (originalRequest != null) {
            return originalRequest.clone();
        }

        try {
            // Get a new request object.  If this can't be done with a default constructor it will fail.
            return requestClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private AmazonWebServiceResult queryNextResults(AmazonWebServiceRequest request) {
        if (clientMethodReturningResult == null) {
            // Look for a public method in the client (AWSIot, etc) that takes a AmazonWebServiceRequest and returns a V.  If zero or more than one exists, fail.
            clientMethodReturningResult = getMethodWithParameterAndReturnType(amazonWebServiceClient.getClass(), requestClass, getResultClass());
        }

        try {
            return (AmazonWebServiceResult) clientMethodReturningResult.invoke(amazonWebServiceClient, request);
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

    private Class<? extends AmazonWebServiceResult> getResultClass() {
        synchronized (this) {
            if (optionalResultClass.isEmpty()) {
                String requestClassName = requestClass.getName();
                String resultClass = requestClassName.replaceAll("Request$", "Result");

                try {
                    optionalResultClass = Option.of((Class<? extends AmazonWebServiceResult>) Class.forName(resultClass));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException(e);
                }
            }
        }

        return optionalResultClass.get();
    }

    private List<T> getResultData() {
        if (clientMethodReturningListT == null) {
            // Look for a public method that takes no arguments and returns a List<T>.  If zero or more than one exists, fail.
            clientMethodReturningListT = getMethodWithParameterAndReturnType(getResultClass(), null, new TypeToken<java.util.List<T>>(getClass()) {
            }.getRawType());
        }

        try {
            return List.ofAll((java.util.List<T>) clientMethodReturningListT.invoke(result));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private String getNextToken() {
        if (clientGetMethodReturningString == null) {
            // Look for a public method that takes no arguments and returns a string that matches our list of expected names.  If zero or more than one exists, fail.
            clientGetMethodReturningString = getMethodWithParameterReturnTypeAndNames(getResultClass(), null, String.class, getTokenMethodNames);
        }

        try {
            return (String) clientGetMethodReturningString.invoke(result);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private void setNextToken(AmazonWebServiceRequest request, String nextToken) {
        if (clientSetMethodAcceptingString == null) {
            // Look for a public method that takes a string and returns nothing that matches our list of expected names.  If zero or more than one exists, fail.
            clientSetMethodAcceptingString = getMethodWithParameterReturnTypeAndNames(requestClass, String.class, Void.TYPE, setTokenMethodNames);
        }

        try {
            clientSetMethodAcceptingString.invoke(request, nextToken);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private Method getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType) {
        return getMethodWithParameterReturnTypeAndName(clazz, parameter, returnType, null);
    }

    private Method getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name) {
        List<String> names = List.empty();

        if (name != null) {
            names = names.append(name);
        }

        return getMethodWithParameterReturnTypeAndNames(clazz, parameter, returnType, names);
    }

    private Method getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names) {
        Method returnMethod = null;

        for (Method method : clazz.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                // Not public, ignore
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

            if (!method.getReturnType().equals(returnType)) {
                // Not the right return type, ignore
                continue;
            }

            if (returnMethod != null) {
                // More than one match found, fail
                throw new UnsupportedOperationException("Multiple methods found, cannot continue");
            }

            returnMethod = method;
        }

        if (returnMethod == null) {
            throw new UnsupportedOperationException("No method found");
        }

        return returnMethod;
    }
}

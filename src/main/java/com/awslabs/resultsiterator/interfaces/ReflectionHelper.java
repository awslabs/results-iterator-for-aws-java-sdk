package com.awslabs.resultsiterator.interfaces;

import io.vavr.collection.List;
import io.vavr.control.Option;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.greengrass.model.GreengrassRequest;
import software.amazon.awssdk.services.greengrass.model.GreengrassResponse;

import java.lang.reflect.Method;

public interface ReflectionHelper {
    Option<Method> getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType);

    Option<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name);

    Option<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names);

    AwsRequest getNewRequest(Class<? extends AwsRequest> awsRequestClass);

    <T extends AwsRequest> T.Builder getNewRequestBuilder(Class<T> awsRequestClass);
}

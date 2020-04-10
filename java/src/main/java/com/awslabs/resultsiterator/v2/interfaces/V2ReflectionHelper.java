package com.awslabs.resultsiterator.v2.interfaces;

import software.amazon.awssdk.awscore.AwsRequest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public interface V2ReflectionHelper {
    Optional<Method> getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType);

    Optional<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name);

    Optional<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names);

    AwsRequest getNewRequest(Class<? extends AwsRequest> awsRequestClass);

    <T extends AwsRequest> T.Builder getNewRequestBuilder(Class<T> awsRequestClass);
}

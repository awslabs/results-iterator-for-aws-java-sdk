package com.awslabs.resultsiterator.interfaces;

import io.vavr.collection.List;
import io.vavr.control.Option;
import software.amazon.awssdk.awscore.AwsRequest;

import java.lang.reflect.Method;

public interface ReflectionHelper {
    Option<Method> getMethodWithParameterAndReturnType(Class clazz, Option<Class> parameterOption, Class returnType);

    Option<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Option<Class> parameterOption, Class returnType, Option<String> nameOption);

    Option<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Option<Class> parameterOption, Class returnType, List<String> names);

    AwsRequest getNewRequest(Class<? extends AwsRequest> awsRequestClass);

    <T extends AwsRequest> T.Builder getNewRequestBuilder(Class<T> awsRequestClass);
}

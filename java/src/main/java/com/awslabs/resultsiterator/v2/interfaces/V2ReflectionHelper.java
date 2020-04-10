package com.awslabs.resultsiterator.v2.interfaces;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public interface V2ReflectionHelper {
    Optional<Method> getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType);

    Optional<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name);

    Optional<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names);
}

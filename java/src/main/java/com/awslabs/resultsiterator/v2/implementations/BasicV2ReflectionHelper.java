package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.resultsiterator.v2.interfaces.V2ReflectionHelper;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BasicV2ReflectionHelper implements V2ReflectionHelper {
    private final List<String> methodsToIgnore = new ArrayList<>(Arrays.asList("sdkFields", "commonPrefixes"));

    @Inject
    public BasicV2ReflectionHelper() {
    }

    @Override
    public Optional<Method> getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType) {
        return getMethodWithParameterReturnTypeAndName(clazz, parameter, returnType, null);
    }

    @Override
    public Optional<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name) {
        List<String> names = new ArrayList<>();

        if (name != null) {
            names.add(name);
        }

        return getMethodWithParameterReturnTypeAndNames(clazz, parameter, returnType, names);
    }

    @Override
    public Optional<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names) {
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

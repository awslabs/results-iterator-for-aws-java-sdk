package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.resultsiterator.v2.interfaces.V2ReflectionHelper;
import io.vavr.control.Try;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.GreengrassRequest;
import software.amazon.awssdk.services.greengrass.model.GreengrassResponse;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BasicV2ReflectionHelper implements V2ReflectionHelper {
    private final List<String> methodsToIgnore = new ArrayList<>(Arrays.asList("sdkFields", "commonPrefixes"));

    @Inject
    GreengrassIdExtractor greengrassIdExtractor;
    @Inject
    GreengrassClient greengrassClient;

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

    @Override
    public AwsRequest getNewRequest(Class<? extends AwsRequest> awsRequestClass) {
        return getNewRequestBuilder(awsRequestClass).build();
    }

    @Override
    public <T extends AwsRequest> T.Builder getNewRequestBuilder(Class<T> awsRequestClass) {
        try {
            // Get a new request object.  If this can't be done without parameters it will fail.
            Method method = awsRequestClass.getMethod("builder");
            return (T.Builder) method.invoke(null);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public <T extends GreengrassResponse> T getSingleGreengrassResult(String versionArn, String prefix, Class<? extends GreengrassRequest> greengrassRequest, Class<T> greengrassResponse) {
        if (versionArn == null) {
            // If no version ARN is available then we need to exit early
            return null;
        }

        AwsRequest.Builder builder = getNewRequestBuilder(greengrassRequest);

        builder = setDefinitionId(builder, prefix, greengrassIdExtractor.extractId(versionArn));
        builder = setDefinitionVersionId(builder, prefix, greengrassIdExtractor.extractVersionId(versionArn));

        AwsRequest request = builder.build();

        Optional<Method> optionalClientMethodReturningResult = getMethodWithParameterAndReturnType(greengrassClient.getClass(), greengrassRequest, greengrassResponse);

        if (!optionalClientMethodReturningResult.isPresent()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected response type, this should never happen.");
        }

        Method clientMethodReturningResult = optionalClientMethodReturningResult.get();

        // This method throws an exception if the definition does not exist
        return (T) Try.of(() -> callMethod(greengrassClient, clientMethodReturningResult, request))
                .getOrNull();
    }

    private AwsRequest.Builder setDefinitionId(AwsRequest.Builder builder, String prefix, String definitionId) {
        return callMethod(builder, String.join("", prefix, "DefinitionId"), definitionId);
    }

    private AwsRequest.Builder setDefinitionVersionId(AwsRequest.Builder builder, String prefix, String definitionVersionId) {
        return callMethod(builder, String.join("", prefix, "DefinitionVersionId"), definitionVersionId);
    }

    private AwsRequest.Builder callMethod(AwsRequest.Builder builder, String methodName, String input) {
        return (AwsRequest.Builder) Try.of(() -> builder.getClass().getMethod(methodName, String.class))
                .mapTry(method -> callMethod(builder, method, input))
                .get();
    }

    private Object callMethod(Object instance, Method method, Object input) {
        return Try.of(() -> setAccessible(method))
                // This is necessary because these methods are not accessible by default
                .mapTry(accessibleMethod -> accessibleMethod.invoke(instance, input))
                .get();
    }

    private Method setAccessible(Method method) {
        // This is necessary because these methods are not accessible by default
        method.setAccessible(true);

        return method;
    }
}

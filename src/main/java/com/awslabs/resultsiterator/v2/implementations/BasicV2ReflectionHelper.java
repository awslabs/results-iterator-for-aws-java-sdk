package com.awslabs.resultsiterator.v2.implementations;

import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;
import com.awslabs.resultsiterator.v2.interfaces.V2ReflectionHelper;
import com.google.gson.internal.$Gson$Types;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.GreengrassRequest;
import software.amazon.awssdk.services.greengrass.model.GreengrassResponse;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.function.Predicate;

public class BasicV2ReflectionHelper implements V2ReflectionHelper {
    private final List<String> methodsToIgnore = List.of("sdkFields", "commonPrefixes", "copy");

    @Inject
    GreengrassIdExtractor greengrassIdExtractor;
    @Inject
    GreengrassClient greengrassClient;

    @Inject
    public BasicV2ReflectionHelper() {
    }

    @Override
    public Option<Method> getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType) {
        return getMethodWithParameterReturnTypeAndName(clazz, parameter, returnType, null);
    }

    @Override
    public Option<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name) {
        List<String> names = List.empty();

        if (name != null) {
            names = names.append(name);
        }

        return getMethodWithParameterReturnTypeAndNames(clazz, parameter, returnType, names);
    }

    @Override
    public Option<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names) {
        // To deal with generics we look at signatures if all else fails. We want to look for lists of the expected type and the expected type itself.
        String expectedListSignature = toGenericListSignature(returnType);
        String expectedSignature = toGenericSignature(returnType);

        Predicate<Method> methodShouldNotBeIgnored = method -> !methodsToIgnore.contains(method.getName());
        Predicate<Method> noNamesSpecifiedOrSpecifiedNameMatches = method -> names.size() == 0 || names.contains(method.getName());
        Predicate<Method> zeroOrOneParametersSpecified = method -> (parameter == null) || (method.getParameterCount() == 1);
        Predicate<Method> zeroParametersOrParameterMatchesExpectedType = method -> (parameter == null) || method.getParameterTypes()[0].equals(parameter);
        Predicate<Method> methodReturnTypeIsAssignableFromReturnType = method -> method.getReturnType().isAssignableFrom(returnType);
        Predicate<Method> expectedListSignatureEqualsGenericSignature = method -> expectedListSignature.equals(toGenericSignature(method.getGenericReturnType()));
        Predicate<Method> expectedSingleValueSignatureEqualsGenericSignature = method -> expectedSignature.equals(toGenericSignature(method.getGenericReturnType()));
        Predicate<Method> returnTypeMatchesOrListOrSingleValueSignatureMatches = methodReturnTypeIsAssignableFromReturnType.or(expectedListSignatureEqualsGenericSignature).or(expectedSingleValueSignatureEqualsGenericSignature);

        List<Method> methodsFound = Stream.of(clazz.getMethods())
                // Only public methods
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                // Only methods that aren't ignored
                .filter(methodShouldNotBeIgnored::test)
                // Either no names were specified or specified name matches
                .filter(noNamesSpecifiedOrSpecifiedNameMatches::test)
                // Either there were zero or one parameters
                .filter(zeroOrOneParametersSpecified::test)
                // If there was a parameter it must match the expected type
                .filter(zeroParametersOrParameterMatchesExpectedType::test)
                // The return type must match OR the signature (list or single value) must match the generic signature
                .filter(returnTypeMatchesOrListOrSingleValueSignatureMatches::test)
                .toList();

        if (methodsFound.size() > 1) {
            // More than one match found, fail
            throw new UnsupportedOperationException("Multiple methods found, cannot continue. Try using V2ResultsIteratorAbstract as an anonymous class to avoid compile time type erasure.");
        }

        return Option.of(methodsFound.getOrNull());
    }

    // From: https://stackoverflow.com/a/29801335/796579
    private String toGenericListSignature(final Type type) {
        ParameterizedType listType = $Gson$Types.newParameterizedTypeWithOwner(null, java.util.List.class, type);
        return toGenericSignature(listType);
    }

    private String toGenericSignature(final Type type) {
        StringBuilder sb = new StringBuilder();
        toGenericSignature(sb, type);
        return sb.toString();
    }

    private void toGenericSignature(StringBuilder sb, final Type type) {
        if (type instanceof GenericArrayType) {
            sb.append("[");
            toGenericSignature(sb, ((GenericArrayType) type).getGenericComponentType());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            sb.append('L');
            sb.append(((Class) pt.getRawType()).getName().replace('.', '/'));
            sb.append('<');
            for (Type p : pt.getActualTypeArguments()) {
                toGenericSignature(sb, p);
            }
            sb.append(">;");
        } else if (type instanceof Class) {
            Class clazz = (Class) type;
            if (!clazz.isPrimitive() && !clazz.isArray()) {
                sb.append('L');
                sb.append(clazz.getName().replace('.', '/'));
                sb.append(';');
            } else {
                sb.append(clazz.getName().replace('.', '/'));
            }
        } else if (type instanceof WildcardType) {
            WildcardType wc = (WildcardType) type;
            Type[] lowerBounds = wc.getLowerBounds();
            Type[] upperBounds = wc.getUpperBounds();
            boolean hasLower = lowerBounds != null && lowerBounds.length > 0;
            boolean hasUpper = upperBounds != null && upperBounds.length > 0;

            if (hasUpper && hasLower && Object.class.equals(lowerBounds[0]) && Object.class.equals(upperBounds[0])) {
                sb.append('*');
            } else if (hasLower) {
                sb.append("-");
                for (Type b : lowerBounds) {
                    toGenericSignature(sb, b);
                }
            } else if (hasUpper) {
                if (upperBounds.length == 1 && Object.class.equals(upperBounds[0])) {
                    sb.append("*");
                } else {
                    sb.append("+");
                    for (Type b : upperBounds) {
                        toGenericSignature(sb, b);
                    }
                }
            } else {
                sb.append('*');
            }
        } else if (type instanceof TypeVariable) {
            // work around: replaces the type variable with it's first bound.
            toGenericSignature(sb, ((TypeVariable) type).getBounds()[0]);
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
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

        Option<Method> clientMethodReturningResultOption = getMethodWithParameterAndReturnType(greengrassClient.getClass(), greengrassRequest, greengrassResponse);

        if (clientMethodReturningResultOption.isEmpty()) {
            throw new UnsupportedOperationException("Failed to find a method returning the expected response type, this should never happen.");
        }

        Method clientMethodReturningResult = clientMethodReturningResultOption.get();

        // callMethod throws an exception if the definition does not exist
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

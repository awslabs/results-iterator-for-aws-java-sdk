package com.awslabs.resultsiterator.implementations;

import com.awslabs.iot.helpers.interfaces.GreengrassV1IdExtractor;
import com.awslabs.resultsiterator.interfaces.ReflectionHelper;
import com.google.gson.internal.$Gson$Types;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.greengrass.GreengrassClient;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.function.Predicate;

public class BasicReflectionHelper implements ReflectionHelper {
    private final List<String> methodsToIgnore = List.of("sdkFields", "commonPrefixes", "copy");

    @Inject
    GreengrassV1IdExtractor greengrassV1IdExtractor;
    @Inject
    GreengrassClient greengrassClient;

    @Inject
    public BasicReflectionHelper() {
    }

    @Override
    public Option<Method> getMethodWithParameterAndReturnType(Class clazz, Option<Class> parameterOption, Class returnType) {
        return getMethodWithParameterReturnTypeAndName(clazz, parameterOption, returnType, Option.none());
    }

    @Override
    public Option<Method> getMethodWithParameterReturnTypeAndName(Class clazz, Option<Class> parameterOption, Class returnType, Option<String> nameOption) {
        return getMethodWithParameterReturnTypeAndNames(clazz, parameterOption, returnType, List.ofAll(nameOption));
    }

    @Override
    public Option<Method> getMethodWithParameterReturnTypeAndNames(Class clazz, Option<Class> parameterOption, Class returnType, List<String> names) {
        // To deal with generics we look at signatures if all else fails. We want to look for lists of the expected type and the expected type itself.
        String expectedListSignature = toGenericListSignature(returnType);
        String expectedSignature = toGenericSignature(returnType);

        Predicate<Method> methodShouldNotBeIgnored = method -> !methodsToIgnore.contains(method.getName());
        Predicate<Method> noNamesSpecifiedOrSpecifiedNameMatches = method -> names.size() == 0 || names.contains(method.getName());
        Predicate<Method> zeroOrOneParametersSpecified = method -> (parameterOption.isEmpty()) || (method.getParameterCount() == 1);
        Predicate<Method> zeroParametersOrParameterMatchesExpectedType = method -> (parameterOption.isEmpty()) || method.getParameterTypes()[0].equals(parameterOption.get());
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
            throw new UnsupportedOperationException("Multiple methods found, cannot continue. Try using ResultsIteratorAbstract as an anonymous class to avoid compile time type erasure.");
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
}

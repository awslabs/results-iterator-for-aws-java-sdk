package com.awslabs.lambda.helpers.interfaces;

import com.awslabs.lambda.data.FunctionAlias;
import com.awslabs.lambda.data.FunctionAliasArn;
import com.awslabs.lambda.data.FunctionName;
import com.awslabs.lambda.data.FunctionVersion;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;

import java.util.Optional;
import java.util.stream.Stream;

public interface V2LambdaHelper {
    Stream<FunctionConfiguration> findFunctionConfigurationsByPartialName(String partialName);

    boolean functionExists(FunctionName functionName);

    Optional<GetFunctionResponse> getFunction(FunctionName functionName);

    boolean aliasExists(FunctionName functionName, FunctionAlias functionAlias);

    FunctionAliasArn createAlias(FunctionName functionName, FunctionVersion functionVersion, FunctionAlias functionAlias);
}

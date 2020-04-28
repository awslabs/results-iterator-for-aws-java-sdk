package com.awslabs.lambda.helpers.interfaces;

import com.awslabs.lambda.data.FunctionAlias;
import com.awslabs.lambda.data.FunctionAliasArn;
import com.awslabs.lambda.data.FunctionName;
import com.awslabs.lambda.data.FunctionVersion;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationResponse;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.PublishVersionResponse;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface V2LambdaHelper {
    Stream<FunctionConfiguration> findFunctionConfigurationsByPartialName(String partialName);

    boolean functionExists(FunctionName functionName);

    boolean functionExists(FunctionAliasArn functionAliasArn);

    Optional<GetFunctionResponse> getFunction(FunctionName functionName);

    Optional<GetFunctionResponse> getFunction(FunctionAliasArn functionAliasArn);

    boolean aliasExists(FunctionName functionName, FunctionAlias functionAlias);

    FunctionAliasArn createAlias(FunctionName functionName, FunctionVersion functionVersion, FunctionAlias functionAlias);

    PublishVersionResponse publishFunctionVersion(FunctionName functionName);

    Map<String, String> getFunctionEnvironment(FunctionName functionName);

    Map<String, String> getFunctionEnvironment(FunctionAliasArn functionAliasArn);

    GetFunctionConfigurationResponse getFunctionConfiguration(FunctionName functionName);

    GetFunctionConfigurationResponse getFunctionConfiguration(FunctionAliasArn functionAliasArn);

    Stream<FunctionConfiguration> getAllFunctionConfigurations();
}

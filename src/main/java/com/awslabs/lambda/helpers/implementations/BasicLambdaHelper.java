package com.awslabs.lambda.helpers.implementations;

import com.awslabs.lambda.data.FunctionVersion;
import com.awslabs.lambda.data.*;
import com.awslabs.lambda.helpers.interfaces.LambdaHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import javax.inject.Inject;
import java.util.regex.Pattern;

public class BasicLambdaHelper implements LambdaHelper {
    private final Logger log = LoggerFactory.getLogger(BasicLambdaHelper.class);

    @Inject
    LambdaClient lambdaClient;

    @Inject
    public BasicLambdaHelper() {
    }

    @Override
    public Stream<FunctionConfiguration> findFunctionConfigurationsByPartialName(String partialName) {
        String partialNameWithoutAlias = partialName.substring(0, partialName.lastIndexOf(":"));
        String escapedPartialName = StringEscapeUtils.escapeJava(partialNameWithoutAlias);
        String patternString = String.join("", "^", escapedPartialName.replaceAll("~", ".*"), "$");
        Pattern pattern = Pattern.compile(patternString);

        return new ResultsIterator<ListFunctionsResponse>(lambdaClient, ListFunctionsRequest.class).stream()
                .map(ListFunctionsResponse::functions)
                .flatMap(Stream::ofAll)
                .filter(function -> pattern.matcher(function.functionName()).find());
    }

    @Override
    public boolean functionExists(FunctionName functionName) {
        return innerFunctionExists(functionName.getName());
    }

    @Override
    public boolean functionExists(FunctionAliasArn functionAliasArn) {
        return innerFunctionExists(functionAliasArn.getAliasArn());
    }

    private boolean innerFunctionExists(String functionNameOrAliasArn) {
        return innerGetFunction(functionNameOrAliasArn).isDefined();
    }

    @Override
    public Option<GetFunctionResponse> getFunction(FunctionName functionName) {
        return innerGetFunction(functionName.getName());
    }

    @Override
    public Option<GetFunctionResponse> getFunction(FunctionAliasArn functionAliasArn) {
        return innerGetFunction(functionAliasArn.getAliasArn());
    }

    private Option<GetFunctionResponse> innerGetFunction(String functionNameOrAliasArn) {
        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(functionNameOrAliasArn)
                .build();

        return Try.of(() -> Option.of(lambdaClient.getFunction(getFunctionRequest)))
                .recover(ResourceNotFoundException.class, throwable -> Option.none())
                .get();
    }

    @Override
    public boolean aliasExists(FunctionName functionName, FunctionAlias functionAlias) {
        GetAliasRequest getAliasRequest = GetAliasRequest.builder()
                .functionName(functionName.getName())
                .name(functionAlias.getAlias())
                .build();

        return Try.of(() -> lambdaClient.getAlias(getAliasRequest) != null)
                .recover(ResourceNotFoundException.class, throwable -> false)
                .get();
    }

    @Override
    public FunctionAliasArn createAlias(FunctionName functionName, FunctionVersion functionVersion, FunctionAlias functionAlias) {
        if (aliasExists(functionName, functionAlias)) {
            log.debug("Deleting existing alias");

            DeleteAliasRequest deleteAliasRequest = DeleteAliasRequest.builder()
                    .functionName(functionName.getName())
                    .name(functionAlias.getAlias())
                    .build();

            lambdaClient.deleteAlias(deleteAliasRequest);
        }

        log.debug(String.join("", "Creating new alias [", functionAlias.getAlias(), "] for version [", functionVersion.getVersion(), "]"));

        CreateAliasRequest createAliasRequest = CreateAliasRequest.builder()
                .functionName(functionName.getName())
                .name(functionAlias.getAlias())
                .functionVersion(functionVersion.getVersion())
                .build();

        CreateAliasResponse createAliasResponse = lambdaClient.createAlias(createAliasRequest);

        return ImmutableFunctionAliasArn.builder().aliasArn(createAliasResponse.aliasArn()).build();
    }

    @Override
    public PublishVersionResponse publishFunctionVersion(FunctionName functionName) {
        PublishVersionRequest publishVersionRequest = PublishVersionRequest.builder()
                .functionName(functionName.getName())
                .build();

        return lambdaClient.publishVersion(publishVersionRequest);
    }

    @Override
    public Map<String, String> getFunctionEnvironment(FunctionName functionName) {
        return innerGetFunctionEnvironment(functionName.getName());
    }

    @Override
    public Map<String, String> getFunctionEnvironment(FunctionAliasArn functionAliasArn) {
        return innerGetFunctionEnvironment(functionAliasArn.getAliasArn());
    }

    private Map<String, String> innerGetFunctionEnvironment(String functionNameOrAliasArn) {
        GetFunctionConfigurationResponse getFunctionConfigurationResponse = innerGetFunctionConfiguration(functionNameOrAliasArn);

        return Option.of(getFunctionConfigurationResponse.environment())
                .map(EnvironmentResponse::variables)
                .map(HashMap::ofAll)
                .getOrElse(HashMap.empty());
    }

    @Override
    public GetFunctionConfigurationResponse getFunctionConfiguration(FunctionName functionName) {
        return innerGetFunctionConfiguration(functionName.getName());
    }

    @Override
    public GetFunctionConfigurationResponse getFunctionConfiguration(FunctionAliasArn functionAliasArn) {
        return innerGetFunctionConfiguration(functionAliasArn.getAliasArn());
    }

    private GetFunctionConfigurationResponse innerGetFunctionConfiguration(String functionNameOrAliasArn) {
        GetFunctionConfigurationRequest getFunctionConfigurationRequest = GetFunctionConfigurationRequest.builder()
                .functionName(functionNameOrAliasArn)
                .build();

        return lambdaClient.getFunctionConfiguration(getFunctionConfigurationRequest);
    }

    @Override
    public Stream<FunctionConfiguration> getAllFunctionConfigurations() {
        return new ResultsIterator<FunctionConfiguration>(lambdaClient, ListFunctionsRequest.class).stream();
    }
}

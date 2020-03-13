package com.awslabs.lambda.helpers.implementations;

import com.awslabs.lambda.data.FunctionVersion;
import com.awslabs.lambda.data.*;
import com.awslabs.lambda.helpers.interfaces.V2LambdaHelper;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import io.vavr.control.Try;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BasicV2LambdaHelper implements V2LambdaHelper {
    private final Logger log = LoggerFactory.getLogger(BasicV2LambdaHelper.class);

    @Inject
    LambdaClient lambdaClient;

    @Inject
    public BasicV2LambdaHelper() {
    }

    @Override
    public Stream<FunctionConfiguration> findFunctionConfigurationsByPartialName(String partialName) {
        String partialNameWithoutAlias = partialName.substring(0, partialName.lastIndexOf(":"));
        String escapedPartialName = StringEscapeUtils.escapeJava(partialNameWithoutAlias);
        String patternString = "^" + escapedPartialName.replaceAll("~", ".*") + "$";
        Pattern pattern = Pattern.compile(patternString);

        return new V2ResultsIterator<ListFunctionsResponse>(lambdaClient, ListFunctionsRequest.class).stream()
                .map(ListFunctionsResponse::functions)
                .flatMap(Collection::stream)
                .filter(function -> pattern.matcher(function.functionName()).find());
    }

    @Override
    public boolean functionExists(FunctionName functionName) {
        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(functionName.getName())
                .build();

        return Try.of(() -> lambdaClient.getFunction(getFunctionRequest) != null)
                .recover(ResourceNotFoundException.class, throwable -> false)
                .get();
    }

    @Override
    public Optional<GetFunctionResponse> getFunction(FunctionName functionName) {
        GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                .functionName(functionName.getName())
                .build();

        return Try.of(() -> Optional.of(lambdaClient.getFunction(getFunctionRequest)))
                .recover(ResourceNotFoundException.class, throwable -> Optional.empty())
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
            log.info("Deleting existing alias");

            DeleteAliasRequest deleteAliasRequest = DeleteAliasRequest.builder()
                    .functionName(functionName.getName())
                    .name(functionAlias.getAlias())
                    .build();

            lambdaClient.deleteAlias(deleteAliasRequest);
        }

        log.info("Creating new alias [" + functionAlias.getAlias() + "] for version [" + functionVersion.getVersion() + "]");

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
}

package com.awslabs.cloudformation.implementations;

import com.awslabs.cloudformation.data.ResourceType;
import com.awslabs.cloudformation.data.StackName;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import io.vavr.Predicates;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import javax.inject.Inject;
import java.util.function.Predicate;

public class BasicCloudFormationHelper implements CloudFormationHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicCloudFormationHelper.class);
    private final Predicate<StackSummary> isCreateComplete = stackSummary -> stackSummary.stackStatus().equals(StackStatus.CREATE_COMPLETE);
    private final Predicate<StackSummary> isUpdateComplete = stackSummary -> stackSummary.stackStatus().equals(StackStatus.UPDATE_COMPLETE);
    @Inject
    CloudFormationClient cloudFormationClient;

    @Inject
    public BasicCloudFormationHelper() {
    }

    @Override
    public Stream<StackSummary> getStackSummaries() {
        return new ResultsIterator<StackSummary>(cloudFormationClient, ListStacksRequest.class).stream();
    }

    @Override
    public Option<StackSummary> getStackSummary(StackName stackName) {
        return getStackSummaries()
                .filter(stackSummary -> Predicates.anyOf(isCreateComplete, isUpdateComplete).test(stackSummary))
                .filter(stackSummary -> stackSummary.stackName().equals(stackName.getStackName()))
                .toOption();
    }

    @Override
    public Stream<StackResourceSummary> getStackResources(StackName stackName) {
        ListStackResourcesRequest listStackResourcesRequest = ListStackResourcesRequest.builder()
                .stackName(stackName.getStackName())
                .build();

        return new ResultsIterator<StackResourceSummary>(cloudFormationClient, listStackResourcesRequest).stream();
    }

    @Override
    public Stream<StackResourceSummary> filterStackResources(Stream<StackResourceSummary> stackResourceSummaryStream, List<ResourceType> resourceTypeList) {
        List<String> resourceTypesByName = resourceTypeList.map(ResourceType::getValue);

        return stackResourceSummaryStream
                .filter(stackResourceSummary -> resourceTypesByName.contains(stackResourceSummary.resourceType()));
    }

    @Override
    public boolean stackExists(StackName stackName) {
        return getStackSummary(stackName).isDefined();
    }

    @Override
    public Option<String> getStackResource(StackName stackName, String resourceType, Option<String> expectedNameOption) {
        DescribeStackResourcesRequest describeStackResourcesRequest = DescribeStackResourcesRequest.builder()
                .stackName(stackName.getStackName())
                .build();

        return new ResultsIterator<StackResource>(cloudFormationClient, describeStackResourcesRequest).stream()
                .filter(stackResource -> stackResource.resourceType().equals(resourceType))
                .filter(stackResource -> expectedNameOption
                        // Remove dashes because they're not allowed
                        .map(value -> value.replaceAll("-", ""))
                        // Make sure the resource starts with the expected string
                        .map(expectedName -> stackResource.logicalResourceId().startsWith(expectedName)).getOrElse(true))
                .map(StackResource::physicalResourceId)
                .toOption();
    }
}

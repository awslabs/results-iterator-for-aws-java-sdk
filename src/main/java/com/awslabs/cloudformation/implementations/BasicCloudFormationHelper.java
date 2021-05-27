package com.awslabs.cloudformation.implementations;

import com.awslabs.cloudformation.data.ResourceType;
import com.awslabs.cloudformation.data.StackName;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import javax.inject.Inject;

public class BasicCloudFormationHelper implements CloudFormationHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicCloudFormationHelper.class);
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
                .filter(stackSummary -> stackSummary.stackStatus().equals(StackStatus.CREATE_COMPLETE))
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
}

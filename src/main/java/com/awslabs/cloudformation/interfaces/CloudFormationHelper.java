package com.awslabs.cloudformation.interfaces;

import com.awslabs.cloudformation.data.ResourceType;
import com.awslabs.cloudformation.data.StackName;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;

public interface CloudFormationHelper {
    Stream<StackSummary> getStackSummaries();

    Option<StackSummary> getStackSummary(StackName stackName);

    Stream<StackResourceSummary> getStackResources(StackName stackName);

    Stream<StackResourceSummary> filterStackResources(Stream<StackResourceSummary> stackResourceSummaryStream, List<ResourceType> resourceTypeList);

    boolean stackExists(StackName stackName);
}

package com.awslabs.cloudformation.interfaces;

import com.awslabs.cloudformation.data.StackName;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;

public interface CloudFormationHelper {
    Stream<StackSummary> getStackSummaries();

    Option<StackSummary> getStackSummary(StackName stackName);

    boolean stackExists(StackName stackName);
}

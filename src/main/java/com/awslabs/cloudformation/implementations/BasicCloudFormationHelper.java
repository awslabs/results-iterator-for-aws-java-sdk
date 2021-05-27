package com.awslabs.cloudformation.implementations;

import com.awslabs.cloudformation.data.StackName;
import com.awslabs.cloudformation.interfaces.CloudFormationHelper;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;

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
    public boolean stackExists(StackName stackName) {
        return getStackSummary(stackName).isDefined();
    }
}

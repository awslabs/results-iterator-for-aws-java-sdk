package com.awslabs.ec2.implementations;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.util.EC2MetadataUtils;
import com.awslabs.ec2.interfaces.V1Ec2Helper;
import com.awslabs.general.helpers.interfaces.AwsHelper;
import io.vavr.control.Option;
import org.slf4j.Logger;

import javax.inject.Inject;

public class BasicV1Ec2Helper implements V1Ec2Helper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1Ec2Helper.class);
    @Inject
    AwsHelper awsHelper;
    @Inject
    AmazonEC2Client amazonEC2Client;

    @Inject
    public BasicV1Ec2Helper() {
    }

    @Override
    public Option<Instance> describeInstance() {
        if (!awsHelper.isEc2()) {
            // No instance ID unless we're on EC2
            return Option.none();
        }

        Option<String> instanceIdOption = Option.of(EC2MetadataUtils.getInstanceId());

        if (instanceIdOption.isEmpty()) {
            // Can't get the instance ID
            return Option.none();
        }

        String instanceId = instanceIdOption.get();

        DescribeInstancesRequest describeAddressesRequest = new DescribeInstancesRequest()
                .withInstanceIds(instanceId);

        DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(describeAddressesRequest);

        Option<Instance> instanceOption = Option.ofOptional(describeInstancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .filter(instance -> instance.getInstanceId().equals(instanceId))
                .findFirst());

        if (instanceOption.isEmpty()) {
            throw new UnsupportedOperationException(String.join("", "Instance ID [", instanceId, "] not found"));
        }

        return instanceOption;
    }
}

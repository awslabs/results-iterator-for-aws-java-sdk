package com.awslabs.ec2.implementations;

import com.awslabs.ec2.interfaces.V2Ec2Helper;
import io.vavr.control.Option;
import org.slf4j.Logger;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;

import javax.inject.Inject;

import static com.awslabs.general.helpers.implementations.AwsHelper.isEc2;

public class BasicV2Ec2Helper implements V2Ec2Helper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV2Ec2Helper.class);
    @Inject
    Ec2Client ec2Client;

    @Inject
    public BasicV2Ec2Helper() {
    }

    @Override
    public Option<Instance> describeInstance() {
        if (!isEc2()) {
            // No instance ID unless we're on EC2
            return Option.none();
        }

        Option<String> instanceIdOption = Option.of(EC2MetadataUtils.getInstanceInfo().getInstanceId());

        if (instanceIdOption.isEmpty()) {
            // Can't get the instance ID
            return Option.none();
        }

        String instanceId = instanceIdOption.get();

        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);

        Option<Instance> instanceOption = Option.ofOptional(describeInstancesResponse.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .filter(instance -> instance.instanceId().equals(instanceId))
                .findFirst());

        if (instanceOption.isEmpty()) {
            throw new UnsupportedOperationException(String.join("", "Instance ID [", instanceId, "] not found"));
        }

        return instanceOption;
    }
}

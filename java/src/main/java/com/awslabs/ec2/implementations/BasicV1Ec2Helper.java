package com.awslabs.ec2.implementations;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.util.EC2MetadataUtils;
import com.awslabs.ec2.interfaces.V1Ec2Helper;
import com.awslabs.general.helpers.interfaces.AwsHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Optional;

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
    public Optional<Instance> describeInstance() {
        if (!awsHelper.isEc2()) {
            // No instance ID unless we're on EC2
            return Optional.empty();
        }

        Optional<String> instanceIdOptional = Optional.ofNullable(EC2MetadataUtils.getInstanceId());

        if (!instanceIdOptional.isPresent()) {
            // Can't get the instance ID
            return Optional.empty();
        }

        String instanceId = instanceIdOptional.get();

        DescribeInstancesRequest describeAddressesRequest = new DescribeInstancesRequest()
                .withInstanceIds(instanceId);

        DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(describeAddressesRequest);

        Optional<Instance> instanceOptional = describeInstancesResult.getReservations().stream()
                .flatMap(reservation -> reservation.getInstances().stream())
                .filter(instance -> instance.getInstanceId().equals(instanceId))
                .findFirst();

        if (!instanceOptional.isPresent()) {
            throw new UnsupportedOperationException("Instance ID [" + instanceId + "] not found");
        }

        return instanceOptional;
    }
}

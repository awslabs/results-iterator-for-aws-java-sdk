package com.awslabs.ec2.interfaces;

import com.amazonaws.services.ec2.model.Instance;

import java.util.Optional;

public interface V1Ec2Helper {
    Optional<Instance> describeInstance();
}

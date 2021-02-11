package com.awslabs.ec2.interfaces;

import io.vavr.control.Option;
import software.amazon.awssdk.services.ec2.model.Instance;

public interface V2Ec2Helper {
    Option<Instance> describeInstance();
}

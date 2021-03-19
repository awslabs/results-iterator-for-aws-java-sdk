package com.awslabs.ec2.interfaces;

import io.vavr.control.Option;
import software.amazon.awssdk.services.ec2.model.Instance;

public interface Ec2Helper {
    Option<Instance> describeInstance();
}

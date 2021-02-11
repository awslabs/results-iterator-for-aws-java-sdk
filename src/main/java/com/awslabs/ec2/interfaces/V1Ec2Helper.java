package com.awslabs.ec2.interfaces;

import com.amazonaws.services.ec2.model.Instance;
import io.vavr.control.Option;

public interface V1Ec2Helper {
    Option<Instance> describeInstance();
}

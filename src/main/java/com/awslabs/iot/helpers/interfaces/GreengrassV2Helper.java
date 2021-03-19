package com.awslabs.iot.helpers.interfaces;

import io.vavr.collection.Stream;
import software.amazon.awssdk.services.greengrassv2.model.Deployment;

public interface GreengrassV2Helper {
    Stream<Deployment> getAllDeployments();
}

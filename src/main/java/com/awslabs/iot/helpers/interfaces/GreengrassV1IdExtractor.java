package com.awslabs.iot.helpers.interfaces;

public interface GreengrassV1IdExtractor {
    String extractId(String arn);

    String extractVersionId(String arn);
}

package com.awslabs.iot.helpers.interfaces;

public interface GreengrassIdExtractor {
    String extractId(String arn);

    String extractVersionId(String arn);
}

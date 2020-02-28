package com.awslabs.iot.helpers.interfaces;

public interface SharedGreengrassIdExtractor {
    String extractId(String arn);

    String extractVersionId(String arn);
}

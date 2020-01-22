package com.awslabs.aws.iot.resultsiterator.helpers.interfaces;

public interface GreengrassIdExtractor {
    String extractId(String arn);

    String extractVersionId(String arn);
}

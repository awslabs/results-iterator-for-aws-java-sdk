package com.awslabs.aws.iot.resultsiterator.helpers;

public interface GreengrassIdExtractor {
    String extractId(String arn);

    String extractVersionId(String arn);
}

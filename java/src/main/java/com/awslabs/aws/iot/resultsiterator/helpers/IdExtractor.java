package com.awslabs.aws.iot.resultsiterator.helpers;

public interface IdExtractor {
    String extractId(String arn);

    String extractVersionId(String arn);
}

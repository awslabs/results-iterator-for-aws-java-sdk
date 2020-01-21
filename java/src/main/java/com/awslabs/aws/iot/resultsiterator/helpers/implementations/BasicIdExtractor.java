package com.awslabs.aws.iot.resultsiterator.helpers.implementations;

import com.awslabs.aws.iot.resultsiterator.helpers.IdExtractor;

import javax.inject.Inject;

public class BasicIdExtractor implements IdExtractor {
    @Inject
    public BasicIdExtractor() {
    }

    @Override
    public String extractId(String arn) {
        return arn.replaceFirst("/versions.*$", "").replaceFirst("^.*/", "");
    }

    @Override
    public String extractVersionId(String arn) {
        return arn.substring(arn.lastIndexOf('/') + 1);
    }
}

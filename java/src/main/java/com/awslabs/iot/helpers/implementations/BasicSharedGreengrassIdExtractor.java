package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.helpers.interfaces.SharedGreengrassIdExtractor;

import javax.inject.Inject;

public class BasicSharedGreengrassIdExtractor implements SharedGreengrassIdExtractor {
    @Inject
    public BasicSharedGreengrassIdExtractor() {
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

package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.helpers.interfaces.GreengrassIdExtractor;

import javax.inject.Inject;

public class BasicGreengrassIdExtractor implements GreengrassIdExtractor {
    @Inject
    public BasicGreengrassIdExtractor() {
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

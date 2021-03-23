package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.helpers.interfaces.GreengrassV1IdExtractor;

import javax.inject.Inject;

public class BasicGreengrassV1IdExtractor implements GreengrassV1IdExtractor {
    @Inject
    public BasicGreengrassV1IdExtractor() {
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

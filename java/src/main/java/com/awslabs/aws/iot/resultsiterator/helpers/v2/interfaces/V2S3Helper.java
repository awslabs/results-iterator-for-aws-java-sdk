package com.awslabs.aws.iot.resultsiterator.helpers.v2.interfaces;

public interface V2S3Helper {
    boolean bucketExists(String bucketName);

    boolean objectExists(String bucket, String key);
}

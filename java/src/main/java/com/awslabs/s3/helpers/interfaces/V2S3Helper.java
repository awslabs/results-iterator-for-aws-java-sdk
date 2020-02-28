package com.awslabs.s3.helpers.interfaces;

public interface V2S3Helper {
    boolean bucketExists(String bucketName);

    boolean objectExists(String bucket, String key);
}

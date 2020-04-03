package com.awslabs.s3.helpers.interfaces;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

public interface V2S3Helper {
    boolean bucketExists(Bucket bucket);

    boolean bucketExists(S3Client s3Client, Bucket bucket);

    boolean objectExists(Bucket bucket, String key);

    boolean objectExists(S3Client s3Client, Bucket bucket, String key);

    S3Client getRegionSpecificClientForBucket(Bucket bucket);
}

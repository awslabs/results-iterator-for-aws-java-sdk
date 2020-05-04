package com.awslabs.s3.helpers.interfaces;

import com.awslabs.s3.helpers.data.S3Bucket;
import com.awslabs.s3.helpers.data.S3Key;
import com.awslabs.s3.helpers.data.S3Path;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;

public interface V2S3Helper {
    boolean bucketExists(Bucket bucket);

    boolean bucketExists(S3Client s3Client, Bucket bucket);

    boolean objectExists(Bucket bucket, String key);

    boolean objectExists(S3Client s3Client, Bucket bucket, String key);

    S3Client getRegionSpecificClientForBucket(Bucket bucket);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, File file);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Key s3Key, File file);
}

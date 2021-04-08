package com.awslabs.s3.helpers.interfaces;

import com.awslabs.s3.helpers.data.S3Bucket;
import com.awslabs.s3.helpers.data.S3Key;
import com.awslabs.s3.helpers.data.S3Path;
import io.vavr.Tuple2;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.net.URI;
import java.net.URL;

public interface S3Helper {
    boolean bucketExists(Bucket bucket);

    boolean bucketExists(S3Client s3Client, Bucket bucket);

    boolean objectExists(Bucket bucket, String key);

    boolean objectExists(S3Client s3Client, Bucket bucket, String key);

    S3Client getRegionSpecificClientForBucket(Bucket bucket);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, File file);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Key s3Key, File file);

    URL getObjectHttpsUrl(S3Bucket s3Bucket, S3Key s3Key);

    URL getObjectHttpsUrl(Tuple2<S3Bucket, S3Key> bucketAndKey);

    URI getObjectS3Uri(S3Bucket s3Bucket, S3Key s3Key);

    URI getObjectS3Uri(Tuple2<S3Bucket, S3Key> bucketAndKey);
}

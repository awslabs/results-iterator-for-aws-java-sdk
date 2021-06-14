package com.awslabs.s3.helpers.interfaces;

import com.awslabs.s3.helpers.data.S3Bucket;
import com.awslabs.s3.helpers.data.S3Key;
import com.awslabs.s3.helpers.data.S3Path;
import io.vavr.Tuple2;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

public interface S3Helper {
    boolean bucketExists(Bucket bucket);

    boolean bucketExists(S3Client s3Client, Bucket bucket);

    boolean objectExists(S3Bucket s3Bucket, S3Key s3Key);

    boolean objectExists(Bucket bucket, String key);

    boolean objectExists(S3Client s3Client, Bucket bucket, String key);

    Region getRegionForBucket(Bucket bucket);

    S3Client getRegionSpecificClientForBucket(Bucket bucket);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, File file);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Key s3Key, File file);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, String filename, InputStream inputStream, int inputStreamLength);

    PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Key s3Key, InputStream inputStream, int inputStreamLength);

    URL getObjectHttpsUrl(S3Bucket s3Bucket, S3Key s3Key);

    URL getObjectHttpsUrl(Tuple2<S3Bucket, S3Key> bucketAndKey);

    URI getObjectS3Uri(S3Bucket s3Bucket, S3Key s3Key);

    URI getObjectS3Uri(Tuple2<S3Bucket, S3Key> bucketAndKey);

    URL presign(S3Bucket s3Bucket, S3Key s3Key, Duration duration);
}

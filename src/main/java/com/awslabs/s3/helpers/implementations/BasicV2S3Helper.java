package com.awslabs.s3.helpers.implementations;

import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.s3.helpers.data.*;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;

public class BasicV2S3Helper implements V2S3Helper {
    @Inject
    Provider<S3Client> s3ClientProvider;
    @Inject
    Provider<S3ClientBuilder> s3ClientBuilderProvider;

    @Inject
    public BasicV2S3Helper() {
    }

    @Override
    public boolean bucketExists(Bucket bucket) {
        return bucketExists(s3ClientProvider.get(), bucket);
    }

    @Override
    public boolean bucketExists(S3Client s3Client, Bucket bucket) {
        GetBucketLocationRequest getBucketLocationRequest = GetBucketLocationRequest.builder()
                .bucket(bucket.name())
                .build();

        return Try.of(() -> s3Client.getBucketLocation(getBucketLocationRequest) != null)
                .recover(NoSuchBucketException.class, false)
                .get();
    }

    @Override
    public boolean objectExists(Bucket bucket, String key) {
        return Try.of(() -> getRegionSpecificClientForBucket(bucket))
                .map(s3Client -> objectExists(s3Client, bucket, key))
                .getOrElse(false);
    }

    @Override
    public boolean objectExists(S3Client s3Client, Bucket bucket, String key) {
        if (!bucketExists(bucket)) {
            return false;
        }

        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket.name())
                .prefix(key)
                .build();

        Stream<S3Object> s3Objects = new V2ResultsIterator<S3Object>(getRegionSpecificClientForBucket(bucket), listObjectsRequest).stream();

        Option<S3Object> s3ObjectOption = Option.of(s3Objects
                // Require an exact match on the name
                .filter(object -> object.key().equals(key))
                .getOrNull());

        return s3ObjectOption.isDefined();
    }

    @Override
    public S3Client getRegionSpecificClientForBucket(Bucket bucket) {
        GetBucketLocationRequest getBucketLocationRequest = GetBucketLocationRequest.builder()
                .bucket(bucket.name())
                .build();

        S3Client defaultS3Client = s3ClientProvider.get();

        return Try.of(() -> defaultS3Client.getBucketLocation(getBucketLocationRequest))
                // If no exception is thrown we're in the right region, just return the existing client
                .map(getBucketLocationResponse -> defaultS3Client)
                // If an exception is thrown attempt to get the region specific client
                .recoverWith(S3Exception.class, this::getRegionSpecificClientForBucketAfterException)
                // Throw an exception if it wasn't handled already
                .get();
    }

    private Try<S3Client> getRegionSpecificClientForBucketAfterException(S3Exception s3Exception) {
        if (!regionIsWrongException(s3Exception)) {
            // This isn't an exception that contains the info we need
            return Try.failure(s3Exception);
        }

        // Extract the region information
        return Try.of(() -> extractRegionFromRegionIsWrongException(s3Exception))
                // Create a new S3 client with the extracted region
                .map(region -> s3ClientBuilderProvider.get().region(region).build());
    }

    private Region extractRegionFromRegionIsWrongException(S3Exception s3Exception) {
        String message = s3Exception.getMessage();

        int regionEndQuoteCharacter = message.lastIndexOf("'");
        int regionBeginQuoteCharacter = message.lastIndexOf("'", regionEndQuoteCharacter - 1);
        String regionString = message.substring(regionBeginQuoteCharacter + 1, regionEndQuoteCharacter);

        return Region.of(regionString);
    }

    private boolean regionIsWrongException(S3Exception s3Exception) {
        String message = s3Exception.getMessage();

        return (message.contains("the region") && message.contains("is wrong"));
    }

    @Override
    public PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, File file) {
        if (s3Path.path().equals("/")) {
            // Clear out the S3 directory if it is just the root
            s3Path = ImmutableS3Path.builder().path("").build();
        }

        String s3FileName = file.getName();

        // Put the key together from the path
        String keyString = String.join("/", s3Path.path(), s3FileName);

        if (keyString.startsWith("/")) {
            // If there's a leading slash remove it
            keyString = keyString.substring(1);
        }

        // Replace any accidental double slashes
        keyString = keyString.replaceAll("//", "/");

        S3Key s3Key = ImmutableS3Key.builder().key(keyString).build();

        return copyToS3(s3Bucket, s3Key, file);
    }

    @Override
    public PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Key s3Key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket.bucket())
                .key(s3Key.key())
                .build();

        return s3ClientProvider.get().putObject(putObjectRequest, RequestBody.fromFile(file));
    }
}

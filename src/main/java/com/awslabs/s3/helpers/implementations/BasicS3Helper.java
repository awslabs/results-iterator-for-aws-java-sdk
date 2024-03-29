package com.awslabs.s3.helpers.implementations;

import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.awslabs.s3.helpers.data.*;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

public class BasicS3Helper implements S3Helper {
    @Inject
    Provider<S3Client> s3ClientProvider;
    @Inject
    Provider<S3ClientBuilder> s3ClientBuilderProvider;
    @Inject
    Provider<S3Utilities> s3UtilitiesProvider;

    @Inject
    public BasicS3Helper() {
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
    public boolean objectExists(S3Bucket s3Bucket, S3Key s3Key) {
        Bucket bucket = Bucket.builder().name(s3Bucket.bucket()).build();
        return objectExists(bucket, s3Key.key());
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

        Stream<S3Object> s3Objects = new ResultsIterator<S3Object>(getRegionSpecificClientForBucket(bucket), listObjectsRequest).stream();

        Option<S3Object> s3ObjectOption = Option.of(s3Objects
                // Require an exact match on the name
                .filter(object -> object.key().equals(key))
                .getOrNull());

        return s3ObjectOption.isDefined();
    }

    @Override
    public Region getRegionForBucket(Bucket bucket) {
        GetBucketLocationRequest getBucketLocationRequest = GetBucketLocationRequest.builder()
                .bucket(bucket.name())
                .build();

        S3Client defaultS3Client = s3ClientProvider.get();

        // Get the bucket location
        return Try.of(() -> defaultS3Client.getBucketLocation(getBucketLocationRequest))
                // The bucket location can be NULL if it is us-east-1
                .map(GetBucketLocationResponse::locationConstraint)
                // Turn the location option into a region object
                .map(this::locationConstraintToRegion)
                // If an exception is thrown attempt to get the region specific client
                .recoverWith(S3Exception.class, this::getRegionForBucketAfterException)
                // Throw an exception if it wasn't handled already
                .get();
    }

    private Region locationConstraintToRegion(BucketLocationConstraint bucketLocationConstraint) {
        if (bucketLocationConstraint.equals(BucketLocationConstraint.UNKNOWN_TO_SDK_VERSION)) {
            return Region.US_EAST_1;
        }

        return Region.of(bucketLocationConstraint.name());
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
        return getRegionForBucketAfterException(s3Exception)
                // Create a new S3 client with the extracted region
                .map(region -> s3ClientBuilderProvider.get().region(region).build());
    }

    private Try<Region> getRegionForBucketAfterException(S3Exception s3Exception) {
        if (!regionIsWrongException(s3Exception)) {
            // This isn't an exception that contains the info we need
            return Try.failure(s3Exception);
        }

        // Extract the region information
        return Try.of(() -> extractRegionFromRegionIsWrongException(s3Exception));
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

    @NotNull
    private S3Key getS3Key(S3Path s3Path, String s3Filename) {
        if (s3Path.path().equals("/")) {
            // Clear out the S3 directory if it is just the root
            s3Path = ImmutableS3Path.builder().path("").build();
        }

        // Put the key together from the path
        String keyString = String.join("/", s3Path.path(), s3Filename);

        if (keyString.startsWith("/")) {
            // If there's a leading slash remove it
            keyString = keyString.substring(1);
        }

        // Replace any accidental double slashes
        keyString = keyString.replaceAll("//", "/");

        S3Key s3Key = ImmutableS3Key.builder().key(keyString).build();
        return s3Key;
    }

    @Override
    public PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, File file) {
        S3Key s3Key = getS3Key(s3Path, file.getName());

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

    @Override
    public PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Path s3Path, String filename, InputStream inputStream, int inputStreamLength) {
        S3Key s3Key = getS3Key(s3Path, filename);

        return copyToS3(s3Bucket, s3Key, inputStream, inputStreamLength);
    }

    @Override
    public PutObjectResponse copyToS3(S3Bucket s3Bucket, S3Key s3Key, InputStream inputStream, int inputStreamLength) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket.bucket())
                .key(s3Key.key())
                .build();

        return s3ClientProvider.get().putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStreamLength));
    }

    @Override
    public URL getObjectHttpsUrl(S3Bucket s3Bucket, S3Key s3Key) {
        GetUrlRequest request = GetUrlRequest.builder().bucket(s3Bucket.bucket()).key(s3Key.key()).build();
        return s3UtilitiesProvider.get().getUrl(request);
    }

    @Override
    public URL getObjectHttpsUrl(Tuple2<S3Bucket, S3Key> bucketAndKey) {
        return getObjectHttpsUrl(bucketAndKey._1, bucketAndKey._2);
    }

    @Override
    public URI getObjectS3Uri(S3Bucket s3Bucket, S3Key s3Key) {
        return Try.of(() -> new URI(String.join("", "s3://", s3Bucket.bucket(), "/", s3Key.key()))).get();
    }

    @Override
    public URI getObjectS3Uri(Tuple2<S3Bucket, S3Key> bucketAndKey) {
        return getObjectS3Uri(bucketAndKey._1, bucketAndKey._2);
    }

    @Override
    public URL presign(S3Bucket s3Bucket, S3Key s3Key, Duration duration) {
        Bucket bucket = Bucket.builder().name(s3Bucket.bucket()).build();

        Region region = getRegionForBucket(bucket);

        // Create a GetObjectRequest to be pre-signed
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucket.name())
                        .key(s3Key.key())
                        .build();

        // Create a GetObjectPresignRequest to specify the signature duration
        GetObjectPresignRequest getObjectPresignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(getObjectRequest)
                        .build();

        S3Presigner s3Presigner = S3Presigner.builder().region(region).build();

        PresignedGetObjectRequest presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);

        return presignedGetObjectRequest.url();
    }
}

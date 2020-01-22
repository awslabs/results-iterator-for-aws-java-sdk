package com.awslabs.aws.iot.resultsiterator;

import com.awslabs.aws.iot.resultsiterator.helpers.v2.V2ResultsIterator;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ThingAttribute;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.greaterThan;

public class TestV2ResultsIterator {
    @Test
    public void shouldObtainThingAttributesStreamAndNotThrowAnException() {
        IotClient iotClient = IotClient.create();
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        V2ResultsIterator<ThingAttribute> v2ResultsIterator = new V2ResultsIterator<>(iotClient, listThingsRequest);
        Stream<ThingAttribute> thingAttributes = v2ResultsIterator.stream();
        thingAttributes.forEach(System.out::println);
        thingAttributes = v2ResultsIterator.stream();
        long count = thingAttributes.count();
        System.out.println("Thing attribute count: " + count);
        Assert.assertThat(count, greaterThan(0L));
    }

    @Test
    public void shouldListBucketsAndNotThrowAnException() {
        S3Client s3Client = S3Client.create();
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        V2ResultsIterator<Bucket> v2ResultsIterator = new V2ResultsIterator<>(s3Client, listBucketsRequest);
        Stream<Bucket> buckets = v2ResultsIterator.stream();
        buckets.forEach(System.out::println);
        buckets = v2ResultsIterator.stream();
        System.out.println("Bucket count: " + buckets.count());
    }

    @Test
    public void shouldListObjectsAndNotThrowAnException() {
        S3Client s3Client = S3Client.create();
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        V2ResultsIterator<Bucket> bucketIterator = new V2ResultsIterator<>(s3Client, listBucketsRequest);
        Stream<Bucket> buckets = bucketIterator.stream();

        buckets.forEach(this::listAll);
    }

    private void listAll(Bucket bucket) {
        // NOTE: Setting the useArnRegionEnabled value does not automatically make cross-region requests. Below is some code to deal with that.
        S3Client s3Client = S3Client.create();
        System.out.println(bucket.name());

        GetBucketLocationRequest getBucketLocationRequest = GetBucketLocationRequest.builder()
                .bucket(bucket.name())
                .build();

        try {
            // If no exception is thrown we're in the right region
            s3Client.getBucketLocation(getBucketLocationRequest);
        } catch (S3Exception e) {
            String message = e.getMessage();

            if (message.contains("the region") && message.contains("is wrong")) {
                int regionEndQuoteCharacter = message.lastIndexOf("'");
                int regionBeginQuoteCharacter = message.lastIndexOf("'", regionEndQuoteCharacter - 1);
                String extractedRegion = message.substring(regionBeginQuoteCharacter + 1, regionEndQuoteCharacter);
                s3Client = S3Client.builder().region(Region.of(extractedRegion)).build();
            }
        }

        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket.name())
                .build();
        V2ResultsIterator<S3Object> s3ObjectIterator = new V2ResultsIterator<>(s3Client, listObjectsRequest);
        Stream<S3Object> s3Objects = s3ObjectIterator.stream();
        s3Objects.forEach(System.out::println);
        s3Objects = s3ObjectIterator.stream();
        System.out.println("Object count: " + s3Objects.count());
    }
}

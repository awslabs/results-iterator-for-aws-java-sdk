package com.awslabs.aws.iot.resultsiterator;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ThingAttribute;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

public class TestResultsIteratorV2 {
    @Test
    public void shouldListThingAttributesAndNotThrowAnException() {
        IotClient iotClient = IotClient.create();
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        ResultsIteratorV2<ThingAttribute> resultsIteratorV2 = new ResultsIteratorV2<>(iotClient, listThingsRequest);
        List<ThingAttribute> thingAttributes = resultsIteratorV2.iterateOverResults();
        thingAttributes.forEach(System.out::println);
        System.out.println("Thing attribute count: " + thingAttributes.size());
    }

    @Test
    public void shouldListBucketsAndNotThrowAnException() {
        S3Client s3Client = S3Client.create();
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ResultsIteratorV2<Bucket> resultsIteratorV2 = new ResultsIteratorV2<>(s3Client, listBucketsRequest);
        List<Bucket> buckets = resultsIteratorV2.iterateOverResults();
        buckets.forEach(System.out::println);
        System.out.println("Bucket count: " + buckets.size());
    }

    @Test
    public void shouldListObjectsAndNotThrowAnException() {
        S3Client s3Client = S3Client.create();
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ResultsIteratorV2<Bucket> bucketIterator = new ResultsIteratorV2<>(s3Client, listBucketsRequest);
        List<Bucket> buckets = bucketIterator.iterateOverResults();

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
        ResultsIteratorV2<S3Object> s3ObjectIterator = new ResultsIteratorV2<>(s3Client, listObjectsRequest);
        List<S3Object> s3Objects = s3ObjectIterator.iterateOverResults();
        s3Objects.forEach(System.out::println);
        System.out.println("Object count: " + s3Objects.size());
    }
}

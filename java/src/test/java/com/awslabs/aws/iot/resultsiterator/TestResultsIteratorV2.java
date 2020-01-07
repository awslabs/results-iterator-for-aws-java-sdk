package com.awslabs.aws.iot.resultsiterator;

import org.junit.Test;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ThingAttribute;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

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
}

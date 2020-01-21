package com.awslabs.aws.iot.resultsiterator;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;

public class TestResultsIterator {
    @Test
    public void shouldListThingAttributesAndNotThrowAnException() {
        AWSIotClient awsIotClient = (AWSIotClient) AWSIotClient.builder().build();
        ListThingsRequest listThingsRequest = new ListThingsRequest();
        ResultsIterator<ThingAttribute> resultsIterator = new ResultsIterator<>(awsIotClient, listThingsRequest);
        List<ThingAttribute> thingAttributes = resultsIterator.iterateOverResults();
        thingAttributes.forEach(System.out::println);
        Assert.assertThat(thingAttributes.size(), greaterThan(0));
    }

    @Test
    @Ignore
    // NOTE: This test fails because the S3 SDK breaks the mold and doesn't return a "Result" object when listing buckets.
    //         Instead it returns the list of buckets directly. This may not be implemented because there's really no reason
    //         to add the complexity into the results iterator for an operation that does not need the iterator.
    public void shouldListBucketsAndNotThrowAnException() {
        AmazonS3Client amazonS3Client = (AmazonS3Client) AmazonS3Client.builder().build();
        ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
        ResultsIterator<Bucket> resultsIterator = new ResultsIterator<>(amazonS3Client, listBucketsRequest);
        List<Bucket> buckets = resultsIterator.iterateOverResults();
        buckets.forEach(System.out::println);
    }
}

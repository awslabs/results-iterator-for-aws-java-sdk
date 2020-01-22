package com.awslabs.aws.iot.resultsiterator;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class TestV1ResultsIterator {
    @Test
    public void shouldObtainThingAttributesStreamAndNotThrowAnException() {
        AWSIotClient awsIotClient = (AWSIotClient) AWSIotClient.builder().build();
        ListThingsRequest listThingsRequest = new ListThingsRequest();
        V1ResultsIterator<ThingAttribute> v1ResultsIterator = new V1ResultsIterator<>(awsIotClient, listThingsRequest);
        Stream<ThingAttribute> thingAttributes = v1ResultsIterator.resultStream();
        thingAttributes.forEach(System.out::println);
        thingAttributes = v1ResultsIterator.resultStream();
        Assert.assertThat(thingAttributes.count(), greaterThan(0L));
    }

    @Test
    public void streamShouldWorkTwice() {
        AWSIotClient awsIotClient = (AWSIotClient) AWSIotClient.builder().build();
        ListThingsRequest listThingsRequest = new ListThingsRequest();
        V1ResultsIterator<ThingAttribute> v1ResultsIterator = new V1ResultsIterator<>(awsIotClient, listThingsRequest);
        Stream<ThingAttribute> thingAttributesStream1 = v1ResultsIterator.resultStream();
        Stream<ThingAttribute> thingAttributesStream2 = v1ResultsIterator.resultStream();
        Assert.assertThat(thingAttributesStream1.count(), equalTo(thingAttributesStream2.count()));
    }

    @Test
    @Ignore
    // NOTE: This test fails because the S3 SDK breaks the mold and doesn't return a "Result" object when listing buckets.
    //         Instead it returns the list of buckets directly. This may not be implemented because there's really no reason
    //         to add the complexity into the results iterator for an operation that does not need the iterator.
    public void shouldListBucketsAndNotThrowAnException() {
        AmazonS3Client amazonS3Client = (AmazonS3Client) AmazonS3Client.builder().build();
        ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
        V1ResultsIterator<Bucket> v1ResultsIterator = new V1ResultsIterator<>(amazonS3Client, listBucketsRequest);
        Stream<Bucket> buckets = v1ResultsIterator.resultStream();
        buckets.forEach(System.out::println);
    }
}

package com.awslabs.resultsiterator;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.CreateThingRequest;
import com.amazonaws.services.iot.model.DeleteThingRequest;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ThingAttribute;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.awslabs.resultsiterator.v1.implementations.DaggerV1TestInjector;
import com.awslabs.resultsiterator.v1.implementations.V1ResultsIterator;
import com.awslabs.resultsiterator.v1.implementations.V1TestInjector;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class TestV1ResultsIterator {
    public static final String JUNKFORTESTING_V1 = "JUNKFORTESTINGv1";
    private final Logger log = LoggerFactory.getLogger(TestV1ResultsIterator.class);
    private AWSIotClient awsIotClient;
    private AmazonS3Client amazonS3Client;

    @Before
    public void setup() {
        V1TestInjector injector = DaggerV1TestInjector.create();
        awsIotClient = injector.awsIotClient();
        amazonS3Client = injector.amazonS3Client();
        CreateThingRequest createThingRequest = new CreateThingRequest()
                .withThingName(JUNKFORTESTING_V1);
        awsIotClient.createThing(createThingRequest);
    }

    @After
    public void tearDown() {
        DeleteThingRequest deleteThingRequest = new DeleteThingRequest()
                .withThingName(JUNKFORTESTING_V1);
        awsIotClient.deleteThing(deleteThingRequest);
    }

    @Test
    public void shouldObtainThingAttributesStreamAndNotThrowAnException() {
        ListThingsRequest listThingsRequest = new ListThingsRequest();
        V1ResultsIterator<ThingAttribute> v1ResultsIterator = new V1ResultsIterator<>(awsIotClient, listThingsRequest);
        Stream<ThingAttribute> thingAttributes = v1ResultsIterator.stream();
        thingAttributes.forEach(System.out::println);
        thingAttributes = v1ResultsIterator.stream();
        MatcherAssert.assertThat(thingAttributes.count(), greaterThan(0L));
    }

    @Test
    public void streamShouldWorkTwice() {
        ListThingsRequest listThingsRequest = new ListThingsRequest();
        V1ResultsIterator<ThingAttribute> v1ResultsIterator = new V1ResultsIterator<>(awsIotClient, listThingsRequest);
        Stream<ThingAttribute> thingAttributesStream1 = v1ResultsIterator.stream();
        Stream<ThingAttribute> thingAttributesStream2 = v1ResultsIterator.stream();
        MatcherAssert.assertThat(thingAttributesStream1.count(), equalTo(thingAttributesStream2.count()));
    }

    @Test
    @Ignore
    // NOTE: This test fails because the S3 SDK breaks the mold and doesn't return a "Result" object when listing buckets.
    //         Instead it returns the list of buckets directly. This may not be implemented because there's really no reason
    //         to add the complexity into the results iterator for an operation that does not need the iterator.
    public void shouldListBucketsAndNotThrowAnException() {
        ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
        V1ResultsIterator<Bucket> v1ResultsIterator = new V1ResultsIterator<>(amazonS3Client, listBucketsRequest);
        Stream<Bucket> buckets = v1ResultsIterator.stream();
        buckets.forEach(System.out::println);
    }
}

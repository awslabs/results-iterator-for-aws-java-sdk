package com.awslabs.resultsiterator;

import com.awslabs.iot.helpers.interfaces.GreengrassV2Helper;
import com.awslabs.resultsiterator.implementations.BasicInjector;
import com.awslabs.resultsiterator.implementations.BouncyCastleCertificateCredentialsProvider;
import com.awslabs.resultsiterator.implementations.DaggerBasicInjector;
import com.awslabs.resultsiterator.implementations.ResultsIterator;
import com.awslabs.resultsiterator.interfaces.CertificateCredentialsProvider;
import com.awslabs.s3.helpers.data.ImmutableS3Bucket;
import com.awslabs.s3.helpers.data.ImmutableS3Key;
import com.awslabs.s3.helpers.data.S3Bucket;
import com.awslabs.s3.helpers.data.S3Key;
import com.awslabs.s3.helpers.interfaces.S3Helper;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.GroupInformation;
import software.amazon.awssdk.services.greengrass.model.ListGroupsRequest;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ThingAttribute;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.general.helpers.implementations.GsonHelper.toJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestResultsIteratorInterface {
    public static final String JUNKFORTHINGTESTING = "JUNKFORTHINGTESTING";
    public static final String JUNKFORGROUPTESTING = "JUNKFORGROUPTESTING";
    // Must be all lowercase
    public static final String JUNKFORECRTESTING = "junkforecrtesting";
    private final Logger log = LoggerFactory.getLogger(TestResultsIteratorInterface.class);
    private IotClient iotClient;
    private GreengrassClient greengrassClient;
    private GreengrassV2Helper greengrassV2Helper;
    private S3Helper s3Helper;
    private S3Client s3Client;
    private CertificateCredentialsProvider certificateCredentialsProvider;

    @Before
    public void setup() {
        BasicInjector injector = DaggerBasicInjector.create();
        iotClient = injector.iotClient();
        greengrassClient = injector.greengrassClient();
        greengrassV2Helper = injector.greengrassV2Helper();
        s3Helper = injector.s3Helper();
        s3Client = injector.s3Client();
        certificateCredentialsProvider = injector.certificateCredentialsProvider();

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING)
                .build();
        iotClient.createThing(createThingRequest);
    }

    @After
    public void tearDown() {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING)
                .build();
        iotClient.deleteThing(deleteThingRequest);
    }

    @Test
    public void shouldObtainThingAttributesStreamAndNotThrowAnException() {
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        ResultsIterator<ThingAttribute> thingAttributesIterator = new ResultsIterator<>(iotClient, listThingsRequest);
        testNotMeaningfulWithout("things", thingAttributesIterator.stream());

        Stream<ThingAttribute> thingAttributes = thingAttributesIterator.stream();
        thingAttributes.map(ThingAttribute::toString).forEach(log::info);
        thingAttributes = thingAttributesIterator.stream();
        int count = thingAttributes.size();
        log.info(String.join(" ", "Thing attribute count:", String.valueOf(count)));
        assertThat(count, greaterThan(0));
    }

    @Test
    public void shouldListBucketsAndNotThrowAnException() {
        ResultsIterator<Bucket> bucketIterator = new ResultsIterator<>(s3Client, ListBucketsRequest.class);
        testNotMeaningfulWithout("buckets", bucketIterator.stream());

        Stream<Bucket> buckets = bucketIterator.stream();
        buckets.map(Bucket::toString).forEach(log::info);
        buckets = bucketIterator.stream();
        log.info(String.join(" ", "Bucket count:", String.valueOf(buckets.size())));
    }

    @Test
    public void shouldListObjectsAndNotThrowAnException() {
        ResultsIterator<Bucket> bucketIterator = new ResultsIterator<>(s3Client, ListBucketsRequest.class);
        testNotMeaningfulWithout("buckets", bucketIterator.stream());

        Stream<Bucket> buckets = bucketIterator.stream();

        int totalNumberOfObjects = buckets.map(this::listAll)
                .size();

        testNotMeaningfulWithout("S3 objects", totalNumberOfObjects);
    }

    @Test
    public void shouldListGreengrassGroupsAndNotThrowAnException() {
        ResultsIterator<GroupInformation> groupInformationIterator = new ResultsIterator<>(greengrassClient, ListGroupsRequest.class);
        testNotMeaningfulWithout("Greengrass groups", groupInformationIterator.stream());

        List<GroupInformation> groupInformationList = List.ofAll(groupInformationIterator.stream());
        groupInformationList.forEach(groupInformation -> log.info(toJson(groupInformation)));
    }

    @Test
    public void streamShouldWorkTwice() {
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        ResultsIterator<ThingAttribute> thingAttributesIterator = new ResultsIterator<>(iotClient, listThingsRequest);
        testNotMeaningfulWithout("things", thingAttributesIterator.stream());

        Stream<ThingAttribute> thingAttributesStream1 = thingAttributesIterator.stream();
        Stream<ThingAttribute> thingAttributesStream2 = thingAttributesIterator.stream();
        assertThat(thingAttributesStream1.size(), equalTo(thingAttributesStream2.size()));
    }

    private long listAll(Bucket bucket) {
        // NOTE: Setting the useArnRegionEnabled value does not automatically make cross-region requests. Below is some code to deal with that.
        log.info(bucket.name());

        ResultsIterator<S3Object> s3ObjectIterator = getS3ObjectResultsIterator(bucket);

        Stream<S3Object> s3Objects = s3ObjectIterator.stream();
        s3Objects.map(S3Object::toString).forEach(log::info);
        s3Objects = s3ObjectIterator.stream();
        long count = s3Objects.size();
        log.info(String.join(" ", "Object count:", String.valueOf(count)));

        return count;
    }

    @NotNull
    private ResultsIterator<S3Object> getS3ObjectResultsIterator(Bucket bucket) {
        S3Client regionSpecificS3Client = s3Helper.getRegionSpecificClientForBucket(bucket);

        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket.name())
                .build();

        return new ResultsIterator<>(regionSpecificS3Client, listObjectsRequest);
    }

    @Test
    public void shouldThrowExceptionWhenThingNameNotPresent() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> certificateCredentialsProvider.resolveCredentials());
        assertTrue(exception.getMessage().contains(BouncyCastleCertificateCredentialsProvider.AWS_CREDENTIAL_PROVIDER_URL));
    }

    @Test
    public void shouldGetTheUrlOfAnS3ObjectAndNotThrowAnException() {
        ResultsIterator<Bucket> bucketIterator = new ResultsIterator<>(s3Client, ListBucketsRequest.class);
        testNotMeaningfulWithout("buckets", bucketIterator.stream());

        Stream<Tuple2<S3Bucket, S3Key>> bucketAndKeyStream = bucketIterator.stream()
                .map(bucket -> Tuple.of(ImmutableS3Bucket.builder().bucket(bucket.name()).build(), getS3ObjectResultsIterator(bucket)))
                .flatMap(tuple -> tuple._2.stream().map(innerValue -> Tuple.of(tuple._1, ImmutableS3Key.builder().key(innerValue.key()).build())));

        testNotMeaningfulWithout("S3 objects", bucketAndKeyStream);

        s3Helper.getObjectHttpsUrl(bucketAndKeyStream.get());
    }
}

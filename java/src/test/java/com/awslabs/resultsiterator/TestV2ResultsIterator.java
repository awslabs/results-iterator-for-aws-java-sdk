package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.resultsiterator.v2.implementations.BouncyCastleV2CertificateCredentialsProvider;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import org.hamcrest.MatcherAssert;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestV2ResultsIterator {
    public static final String JUNKFORTESTING_V2 = "JUNKFORTESTINGv2";
    private final Logger log = LoggerFactory.getLogger(TestV2ResultsIterator.class);
    private IotClient iotClient;
    private GreengrassClient greengrassClient;
    private V2S3Helper v2S3Helper;
    private S3Client s3Client;
    private V2CertificateCredentialsProvider v2CertificateCredentialsProvider;
    private V2GreengrassHelper v2GreengrassHelper;
    private JsonHelper jsonHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        iotClient = injector.iotClient();
        greengrassClient = injector.greengrassClient();
        v2S3Helper = injector.v2S3Helper();
        s3Client = injector.s3Client();
        v2CertificateCredentialsProvider = injector.v2CertificateCredentialsProvider();
        v2GreengrassHelper = injector.v2GreengrassHelper();
        jsonHelper = injector.jsonHelper();

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(JUNKFORTESTING_V2)
                .build();
        iotClient.createThing(createThingRequest);
    }

    @After
    public void tearDown() {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(JUNKFORTESTING_V2)
                .build();
        iotClient.deleteThing(deleteThingRequest);
    }

    @Test
    public void shouldObtainThingAttributesStreamAndNotThrowAnException() {
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        V2ResultsIterator<ThingAttribute> thingAttributesIterator = new V2ResultsIterator<>(iotClient, listThingsRequest);
        assertTrue(testNotMeaningfulWithout("things"), streamNotEmpty(thingAttributesIterator.stream()));

        Stream<ThingAttribute> thingAttributes = thingAttributesIterator.stream();
        thingAttributes.map(ThingAttribute::toString).forEach(log::info);
        thingAttributes = thingAttributesIterator.stream();
        long count = thingAttributes.count();
        log.info("Thing attribute count: " + count);
        MatcherAssert.assertThat(count, greaterThan(0L));
    }

    @Test
    public void shouldListBucketsAndNotThrowAnException() {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        V2ResultsIterator<Bucket> bucketIterator = new V2ResultsIterator<>(s3Client, listBucketsRequest);
        assertTrue(testNotMeaningfulWithout("buckets"), streamNotEmpty(bucketIterator.stream()));

        Stream<Bucket> buckets = bucketIterator.stream();
        buckets.forEach(System.out::println);
        buckets = bucketIterator.stream();
        System.out.println("Bucket count: " + buckets.count());
    }

    @Test
    public void shouldListObjectsAndNotThrowAnException() {
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        V2ResultsIterator<Bucket> bucketIterator = new V2ResultsIterator<>(s3Client, listBucketsRequest);
        assertTrue(testNotMeaningfulWithout("buckets"), streamNotEmpty(bucketIterator.stream()));

        Stream<Bucket> buckets = bucketIterator.stream();

        Long totalNumberOfObjects = buckets.map(this::listAll)
                .reduce(0L, Long::sum);

        assertTrue(testNotMeaningfulWithout("S3 objects"), totalNumberOfObjects > 0);
    }

    @Test
    public void shouldListGreengrassGroupsAndNotThrowAnException() {
        V2ResultsIterator<GroupInformation> groupInformationIterator = new V2ResultsIterator<>(greengrassClient, ListGroupsRequest.class);
        assertTrue(testNotMeaningfulWithout("Greengrass groups"), streamNotEmpty(groupInformationIterator.stream()));

        List<GroupInformation> groupInformationList = groupInformationIterator.stream().collect(Collectors.toList());
        groupInformationList.forEach(groupInformation -> log.info(jsonHelper.toJson(groupInformation)));
    }

    @Test
    public void shouldListGreengrassGroupSubscriptionsAndNotThrowAnException() {
        V2ResultsIterator<GroupInformation> groupInformationIterator = new V2ResultsIterator<>(greengrassClient, ListGroupsRequest.class);
        assertTrue(testNotMeaningfulWithout("Greengrass groups"), streamNotEmpty(groupInformationIterator.stream()));

        List<GroupInformation> groupInformationList = groupInformationIterator.stream().collect(Collectors.toList());

        groupInformationList.stream()
                .map(groupInformation -> v2GreengrassHelper.getSubscriptions(groupInformation))
                .forEach(subscriptions -> log.info(jsonHelper.toJson(subscriptions)));
    }

    @Test
    public void streamShouldWorkTwice() {
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        V2ResultsIterator<ThingAttribute> thingAttributesIterator = new V2ResultsIterator<>(iotClient, listThingsRequest);
        assertTrue(testNotMeaningfulWithout("things"), streamNotEmpty(thingAttributesIterator.stream()));

        Stream<ThingAttribute> thingAttributesStream1 = thingAttributesIterator.stream();
        Stream<ThingAttribute> thingAttributesStream2 = thingAttributesIterator.stream();
        MatcherAssert.assertThat(thingAttributesStream1.count(), equalTo(thingAttributesStream2.count()));
    }

    private long listAll(Bucket bucket) {
        // NOTE: Setting the useArnRegionEnabled value does not automatically make cross-region requests. Below is some code to deal with that.
        System.out.println(bucket.name());

        S3Client regionSpecificS3Client = v2S3Helper.getRegionSpecificClientForBucket(bucket);

        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket.name())
                .build();

        V2ResultsIterator<S3Object> s3ObjectIterator = new V2ResultsIterator<>(regionSpecificS3Client, listObjectsRequest);

        Stream<S3Object> s3Objects = s3ObjectIterator.stream();
        s3Objects.map(S3Object::toString).forEach(log::info);
        s3Objects = s3ObjectIterator.stream();
        long count = s3Objects.count();
        log.info("Object count: " + count);

        return count;
    }

    @Test
    public void shouldThrowExceptionWhenThingNameNotPresent() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> v2CertificateCredentialsProvider.resolveCredentials());
        assertTrue(exception.getMessage().contains(BouncyCastleV2CertificateCredentialsProvider.AWS_CREDENTIAL_PROVIDER_URL));
    }

    private String testNotMeaningfulWithout(String nameOfRequiredObjects) {
        return String.join(" ", "This test is not meaningful unless one or more", nameOfRequiredObjects, "are defined");
    }

    private boolean streamNotEmpty(Stream stream) {
        return stream.findFirst().isPresent();
    }
}

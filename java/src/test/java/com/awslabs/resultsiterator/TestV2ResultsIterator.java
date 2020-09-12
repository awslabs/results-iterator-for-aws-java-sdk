package com.awslabs.resultsiterator;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.resultsiterator.v2.implementations.*;
import com.awslabs.resultsiterator.v2.interfaces.V2CertificateCredentialsProvider;
import com.awslabs.s3.helpers.interfaces.V2S3Helper;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.GroupInformation;
import software.amazon.awssdk.services.greengrass.model.ListGroupsRequest;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestV2ResultsIterator {
    public static final String JUNKFORTHINGTESTING_V2 = "JUNKFORTHINGTESTINGv2";
    public static final String JUNKFORGROUPTESTING_V2 = "JUNKFORGROUPTESTINGv2";
    public static final String THING_GROUP_NAMES = "thingGroupNames";
    public static final String DELIMITER = ":";
    private final Logger log = LoggerFactory.getLogger(TestV2ResultsIterator.class);
    private IotClient iotClient;
    private GreengrassClient greengrassClient;
    private V2S3Helper v2S3Helper;
    private S3Client s3Client;
    private V2CertificateCredentialsProvider v2CertificateCredentialsProvider;
    private JsonHelper jsonHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        iotClient = injector.iotClient();
        greengrassClient = injector.greengrassClient();
        v2S3Helper = injector.v2S3Helper();
        s3Client = injector.s3Client();
        v2CertificateCredentialsProvider = injector.v2CertificateCredentialsProvider();
        jsonHelper = injector.jsonHelper();

        CreateThingRequest createThingRequest = CreateThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING_V2)
                .build();
        iotClient.createThing(createThingRequest);

        CreateThingGroupRequest createThingGroupRequest = CreateThingGroupRequest.builder()
                .thingGroupName(JUNKFORGROUPTESTING_V2)
                .build();
        iotClient.createThingGroup(createThingGroupRequest);

        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING_V2)
                .build();

        DescribeThingResponse describeThingResponse = iotClient.describeThing(describeThingRequest);

        AddThingToThingGroupRequest addThingToThingGroupRequest = AddThingToThingGroupRequest.builder()
                .thingArn(describeThingResponse.thingArn())
                .thingGroupName(JUNKFORGROUPTESTING_V2)
                .build();
        iotClient.addThingToThingGroup(addThingToThingGroupRequest);
    }

    @After
    public void tearDown() {
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
                .thingName(JUNKFORTHINGTESTING_V2)
                .build();
        iotClient.deleteThing(deleteThingRequest);

        DeleteThingGroupRequest deleteThingGroupRequest = DeleteThingGroupRequest.builder()
                .thingGroupName(JUNKFORGROUPTESTING_V2)
                .build();
        iotClient.deleteThingGroup(deleteThingGroupRequest);
    }

    @Test
    public void shouldObtainThingAttributesStreamAndNotThrowAnException() {
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        V2ResultsIterator<ThingAttribute> thingAttributesIterator = new V2ResultsIterator<>(iotClient, listThingsRequest);
        testNotMeaningfulWithout("things", thingAttributesIterator.stream());

        Stream<ThingAttribute> thingAttributes = thingAttributesIterator.stream();
        thingAttributes.map(ThingAttribute::toString).forEach(log::info);
        thingAttributes = thingAttributesIterator.stream();
        long count = thingAttributes.count();
        log.info(String.join(" ", "Thing attribute count:", String.valueOf(count)));
        MatcherAssert.assertThat(count, greaterThan(0L));
    }

    @Test
    public void shouldListBucketsAndNotThrowAnException() {
        V2ResultsIterator<Bucket> bucketIterator = new V2ResultsIterator<>(s3Client, ListBucketsRequest.class);
        testNotMeaningfulWithout("buckets", bucketIterator.stream());

        Stream<Bucket> buckets = bucketIterator.stream();
        buckets.map(Bucket::toString).forEach(log::info);
        buckets = bucketIterator.stream();
        log.info(String.join(" ", "Bucket count:", String.valueOf(buckets.count())));
    }

    @Test
    public void shouldListObjectsAndNotThrowAnException() {
        V2ResultsIterator<Bucket> bucketIterator = new V2ResultsIterator<>(s3Client, ListBucketsRequest.class);
        testNotMeaningfulWithout("buckets", bucketIterator.stream());

        Stream<Bucket> buckets = bucketIterator.stream();

        Long totalNumberOfObjects = buckets.map(this::listAll)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("S3 objects", totalNumberOfObjects);
    }

    @Test
    public void shouldListGreengrassGroupsAndNotThrowAnException() {
        V2ResultsIterator<GroupInformation> groupInformationIterator = new V2ResultsIterator<>(greengrassClient, ListGroupsRequest.class);
        testNotMeaningfulWithout("Greengrass groups", groupInformationIterator.stream());

        List<GroupInformation> groupInformationList = groupInformationIterator.stream().collect(Collectors.toList());
        groupInformationList.forEach(groupInformation -> log.info(jsonHelper.toJson(groupInformation)));
    }

    @Test
    public void streamShouldWorkTwice() {
        ListThingsRequest listThingsRequest = ListThingsRequest.builder().build();
        V2ResultsIterator<ThingAttribute> thingAttributesIterator = new V2ResultsIterator<>(iotClient, listThingsRequest);
        testNotMeaningfulWithout("things", thingAttributesIterator.stream());

        Stream<ThingAttribute> thingAttributesStream1 = thingAttributesIterator.stream();
        Stream<ThingAttribute> thingAttributesStream2 = thingAttributesIterator.stream();
        MatcherAssert.assertThat(thingAttributesStream1.count(), equalTo(thingAttributesStream2.count()));
    }

    private long listAll(Bucket bucket) {
        // NOTE: Setting the useArnRegionEnabled value does not automatically make cross-region requests. Below is some code to deal with that.
        log.info(bucket.name());

        S3Client regionSpecificS3Client = v2S3Helper.getRegionSpecificClientForBucket(bucket);

        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(bucket.name())
                .build();

        V2ResultsIterator<S3Object> s3ObjectIterator = new V2ResultsIterator<>(regionSpecificS3Client, listObjectsRequest);

        Stream<S3Object> s3Objects = s3ObjectIterator.stream();
        s3Objects.map(S3Object::toString).forEach(log::info);
        s3Objects = s3ObjectIterator.stream();
        long count = s3Objects.count();
        log.info(String.join(" ", "Object count:", String.valueOf(count)));

        return count;
    }

    @Test
    public void shouldThrowExceptionWhenThingNameNotPresent() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> v2CertificateCredentialsProvider.resolveCredentials());
        assertTrue(exception.getMessage().contains(BouncyCastleV2CertificateCredentialsProvider.AWS_CREDENTIAL_PROVIDER_URL));
    }

    private void waitForNonZeroFleetIndexResult(Callable<Stream> streamCallable) {
        RetryPolicy<Long> fleetIndexRetryPolicy = new RetryPolicy<Long>()
                .handleResult(0L)
                .withDelay(Duration.ofSeconds(5))
                .withMaxRetries(10)
                .onRetry(failure -> log.warn("Waiting for non-zero fleet index result..."))
                .onRetriesExceeded(failure -> log.error("Fleet index never returned results, giving up"));

        Failsafe.with(fleetIndexRetryPolicy).get(() -> streamCallable.call().count());
    }

    @Test
    public void shouldGetThingDocumentsInsteadOfThingGroups() {
        SearchIndexRequest searchIndexRequest = getAllThingGroupsSearchIndexRequest();

        V2ResultsIteratorAbstract<ThingDocument> thingDocumentsIterator = new V2ResultsIteratorAbstract<ThingDocument>(iotClient, searchIndexRequest) {
        };

        // Wait for the fleet index to settle
        waitForNonZeroFleetIndexResult(thingDocumentsIterator::stream);

        testNotMeaningfulWithout("things in thing groups", thingDocumentsIterator.stream());
    }

    private SearchIndexRequest getAllThingGroupsSearchIndexRequest() {
        String queryString = String.join(DELIMITER, THING_GROUP_NAMES, JUNKFORGROUPTESTING_V2);

        return SearchIndexRequest.builder()
                .queryString(queryString)
                .build();
    }

    @Test
    public void shouldThrowExceptionDueToTypeErasureAmbiguityWhenRequestingSearchIndexResults() {
        SearchIndexRequest searchIndexRequest = getAllThingGroupsSearchIndexRequest();

        V2ResultsIterator<ThingDocument> thingDocumentsIterator = new V2ResultsIterator<>(iotClient, searchIndexRequest);
        RuntimeException unsupportedOperationException = Assert.assertThrows(UnsupportedOperationException.class, () -> thingDocumentsIterator.stream().count());
        Assert.assertTrue(unsupportedOperationException.getMessage().contains("Multiple methods found"));
    }
}

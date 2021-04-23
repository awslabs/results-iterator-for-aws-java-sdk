package com.awslabs.sqs.helpers.implementations;

import com.awslabs.resultsiterator.implementations.BasicInjector;
import com.awslabs.resultsiterator.implementations.DaggerBasicInjector;
import com.awslabs.sqs.data.ImmutableQueueName;
import com.awslabs.sqs.data.QueueName;
import com.awslabs.sqs.data.QueueUrl;
import com.awslabs.sqs.helpers.interfaces.SqsHelper;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.QueueDeletedRecentlyException;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.LongStream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static com.awslabs.general.helpers.implementations.GsonHelper.toJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasicSqsHelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicSqsHelperTests.class);
    private SqsHelper sqsHelper;

    @Before
    public void setup() {
        BasicInjector injector = DaggerBasicInjector.create();
        sqsHelper = injector.sqsHelper();
    }

    @Test
    public void shouldListQueueUrlsAndNotThrowAnException() throws Exception {
        Callable<Stream<QueueUrl>> getQueueUrlsStream = () -> sqsHelper.getQueueUrls();
        testNotMeaningfulWithout("queues", getQueueUrlsStream.call());

        logStreamData(getQueueUrlsStream);
    }

    private <T> void logStreamData(Callable<Stream<T>> getStream) throws Exception {
        getStream.call().forEach(object -> log.info(toJson(object)));
    }

    @Test
    public void shouldCreateAndDeleteQueues() {
        // Create 100 queues, with consistent names
        int expectedCount = 100;

        List<QueueName> queueNames = List.ofAll(LongStream.range(0, expectedCount)
                // Convert the long to a byte array so it can be hashed into a consistent queue name
                .mapToObj(this::longToBytes)
                // Create a queue name as the UUID by hashing the bytes
                .map(UUID::nameUUIDFromBytes)
                // Convert the UUID to a string
                .map(UUID::toString)
                // Convert the string to a QueueName object
                .map(queueName -> ImmutableQueueName.builder().name(queueName).build()));

        // Create all of the queues
        RetryPolicy<QueueUrl> sqsCreateQueuesRetryPolicy = new RetryPolicy<QueueUrl>()
                .withDelay(Duration.ofSeconds(10))
                .withMaxRetries(6)
                .handle(QueueDeletedRecentlyException.class)
                .onRetry(failure -> log.warn("Waiting for SQS to allow recreation of the queue..."))
                .onRetriesExceeded(failure -> log.error("SQS never allowed the queue to be recreated, giving up"));

        List<QueueUrl> queueUrls = queueNames
                .map(queueName -> Failsafe.with(sqsCreateQueuesRetryPolicy).get(() -> sqsHelper.createQueue(queueName)));

        RetryPolicy<Integer> sqsGetQueueUrlsRetryPolicy = new RetryPolicy<Integer>()
                .handleResult(0)
                .withDelay(Duration.ofSeconds(5))
                .withMaxRetries(10)
                .handle(QueueDeletedRecentlyException.class)
                .onRetry(failure -> log.warn("Waiting for non-zero queue URL list result..."))
                .onRetriesExceeded(failure -> log.error("SQS never returned results, giving up"));

        // Count the number of created queues (this can fail if you have queues with UUIDs as names)
        int actualCount = Failsafe.with(sqsGetQueueUrlsRetryPolicy).get(() ->
                sqsHelper.getQueueUrls()
                        .filter(getUuidPredicate())
                        .size());

        // Make sure the count matches
        assertThat(actualCount, is(expectedCount));

        // Delete the queues we created
        queueUrls.forEach(queueUrl -> sqsHelper.deleteQueue(queueUrl));
    }

    private Predicate<QueueUrl> getUuidPredicate() {
        return queueUrl -> queueUrl.getQueueName().getName().matches("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}");
    }

    public byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }
}


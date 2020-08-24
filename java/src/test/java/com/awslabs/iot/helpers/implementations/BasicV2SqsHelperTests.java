package com.awslabs.iot.helpers.implementations;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import com.awslabs.sqs.data.ImmutableQueueName;
import com.awslabs.sqs.data.QueueName;
import com.awslabs.sqs.data.QueueUrl;
import com.awslabs.sqs.helpers.interfaces.V2SqsHelper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasicV2SqsHelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicV2SqsHelperTests.class);
    private V2SqsHelper v2SqsHelper;
    private JsonHelper jsonHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        v2SqsHelper = injector.v2SqsHelper();
        jsonHelper = injector.jsonHelper();
    }

    @Test
    public void shouldListQueueUrlsAndNotThrowAnException() throws Exception {
        Callable<Stream<QueueUrl>> getQueueUrlsStream = () -> v2SqsHelper.getQueueUrls();
        testNotMeaningfulWithout("queues", getQueueUrlsStream.call());

        logStreamData(getQueueUrlsStream);
    }

    private <T> void logStreamData(Callable<Stream<T>> getStream) throws Exception {
        getStream.call().forEach(object -> log.info(jsonHelper.toJson(object)));
    }

    @Test
    public void shouldCreateAndDeleteQueues() {
        // Create 100 queues, with consistent names
        long expectedCount = 100;

        List<QueueName> queueNames = LongStream.range(0, expectedCount)
                // Convert the long to a byte array so it can be hashed into a consistent queue name
                .mapToObj(this::longToBytes)
                // Create a queue name as the UUID by hashing the bytes
                .map(UUID::nameUUIDFromBytes)
                // Convert the UUID to a string
                .map(UUID::toString)
                // Convert the string to a QueueName object
                .map(queueName -> ImmutableQueueName.builder().name(queueName).build())
                .collect(Collectors.toList());

        // Create all of the queues
        List<QueueUrl> queueUrls = queueNames.stream()
                .map(queueName -> v2SqsHelper.createQueue(queueName))
                .collect(Collectors.toList());

        // Count the number of created queues (this can fail if you have queues with UUIDs as names)
        long actualCount = v2SqsHelper.getQueueUrls()
                .filter(getUuidPredicate())
                .count();

        // Make sure the count matches
        assertThat(actualCount, is(expectedCount));

        // Delete the queues we created
        queueUrls.forEach(queueUrl -> v2SqsHelper.deleteQueue(queueUrl));
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


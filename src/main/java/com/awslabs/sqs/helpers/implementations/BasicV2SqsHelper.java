package com.awslabs.sqs.helpers.implementations;

import com.awslabs.resultsiterator.v2.implementations.V2ResultsIterator;
import com.awslabs.sqs.data.*;
import com.awslabs.sqs.helpers.interfaces.V2SqsHelper;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.*;

import javax.inject.Inject;
import javax.inject.Provider;

public class BasicV2SqsHelper implements V2SqsHelper {
    private final Logger log = LoggerFactory.getLogger(BasicV2SqsHelper.class);

    @Inject
    Provider<SqsClientBuilder> sqsClientBuilderProvider;
    @Inject
    AwsRegionProviderChain awsRegionProviderChain;

    @Inject
    public BasicV2SqsHelper() {
    }

    @Override
    public List<Message> receiveMessage(QueueUrl queueUrl, VisibilityTimeout visibilityTimeout) {
        return receiveMessages(queueUrl, Option.of(visibilityTimeout), Option.of(ImmutableMaxNumberOfMessages.builder().value(1).build()));
    }

    @Override
    public List<Message> receiveMessages(QueueUrl queueUrl, VisibilityTimeout visibilityTimeout, MaxNumberOfMessages maxNumberOfMessages) {
        return receiveMessages(queueUrl, Option.of(visibilityTimeout), Option.of(maxNumberOfMessages));
    }

    @Override
    public List<Message> receiveMessages(QueueUrl queueUrl, Option<VisibilityTimeout> optionalVisibilityTimeout, Option<MaxNumberOfMessages> optionalMaxNumberOfMessages) {
        ReceiveMessageRequest.Builder receiveMessageRequestBuilder = ReceiveMessageRequest.builder();
        receiveMessageRequestBuilder.queueUrl(queueUrl.getUrl());
        optionalVisibilityTimeout.forEach(visibilityTimeout -> receiveMessageRequestBuilder.visibilityTimeout(Math.toIntExact(visibilityTimeout.getDuration().getSeconds())));
        optionalMaxNumberOfMessages.forEach(maxNumberOfMessages -> receiveMessageRequestBuilder.maxNumberOfMessages(maxNumberOfMessages.getValue()));

        ReceiveMessageResponse receiveMessageResponse = getRegionSpecificClientForQueue(queueUrl).receiveMessage(receiveMessageRequestBuilder.build());
        return List.ofAll(receiveMessageResponse.messages());
    }

    @Override
    public void deleteMessage(QueueUrl queueUrl, ReceiptHandle receiptHandle) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl.getUrl())
                .receiptHandle(receiptHandle.getHandle())
                .build();

        getRegionSpecificClientForQueue(queueUrl).deleteMessage(deleteMessageRequest);
    }

    @Override
    public Stream<QueueUrl> getQueueUrls() {
        return new V2ResultsIterator<String>(getDefaultSqsClient(), ListQueuesRequest.class).stream()
                .map(queueUrl -> ImmutableQueueUrl.builder().url(queueUrl).build());
    }

    @Override
    public void deleteQueue(QueueUrl queueUrl) {
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(queueUrl.getUrl())
                .build();

        getDefaultSqsClient().deleteQueue(deleteQueueRequest);
    }

    @Override
    public QueueUrl createQueue(QueueName queueName) {
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(queueName.getName())
                .build();

        return ImmutableQueueUrl.builder().url(getDefaultSqsClient().createQueue(createQueueRequest).queueUrl()).build();
    }

    private SqsClient getRegionSpecificClientForQueue(QueueUrl queueUrl) {
        Region currentRegion = awsRegionProviderChain.getRegion();

        // SQS queue URLs look like this: "https://sqs.REGION.amazonaws.com/xxxxxxxxxxxx/yyyyyyyyyy
        String url = queueUrl.getUrl();

        url = url.replace("https://", "");
        String[] urlComponents = url.split("\\.");

        if (urlComponents.length < 4) {
            throw new RuntimeException("Could not parse the region out of the queue URL [" + queueUrl.getUrl() + "]");
        }

        String queueRegionString = urlComponents[1];

        Region queueRegion = Region.of(queueRegionString);

        if (!queueRegion.equals(currentRegion)) {
            return sqsClientBuilderProvider.get().region(queueRegion).build();
        } else {
            return getDefaultSqsClient();
        }
    }

    private SqsClient getDefaultSqsClient() {
        return sqsClientBuilderProvider.get().build();
    }
}

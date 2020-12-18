package com.awslabs.sqs.helpers.interfaces;

import com.awslabs.sqs.data.*;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface V2SqsHelper {
    List<Message> receiveMessage(QueueUrl queueUrl, VisibilityTimeout visibilityTimeout);

    List<Message> receiveMessages(QueueUrl queueUrl, VisibilityTimeout visibilityTimeout, MaxNumberOfMessages maxNumberOfMessages);

    List<Message> receiveMessages(QueueUrl queueUrl, Optional<VisibilityTimeout> optionalVisibilityTimeout, Optional<MaxNumberOfMessages> optionalMaxNumberOfMessages);

    void deleteMessage(QueueUrl queueUrl, ReceiptHandle receiptHandle);

    Stream<QueueUrl> getQueueUrls();

    void deleteQueue(QueueUrl queueUrl);

    QueueUrl createQueue(QueueName queueName);
}

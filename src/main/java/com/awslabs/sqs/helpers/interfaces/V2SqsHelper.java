package com.awslabs.sqs.helpers.interfaces;

import com.awslabs.sqs.data.*;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import software.amazon.awssdk.services.sqs.model.Message;

public interface V2SqsHelper {
    List<Message> receiveMessage(QueueUrl queueUrl, VisibilityTimeout visibilityTimeout);

    List<Message> receiveMessages(QueueUrl queueUrl, VisibilityTimeout visibilityTimeout, MaxNumberOfMessages maxNumberOfMessages);

    List<Message> receiveMessages(QueueUrl queueUrl, Option<VisibilityTimeout> optionalVisibilityTimeout, Option<MaxNumberOfMessages> optionalMaxNumberOfMessages);

    void deleteMessage(QueueUrl queueUrl, ReceiptHandle receiptHandle);

    Stream<QueueUrl> getQueueUrls();

    void deleteQueue(QueueUrl queueUrl);

    QueueUrl createQueue(QueueName queueName);
}

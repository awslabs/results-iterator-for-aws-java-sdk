package com.awslabs.sqs.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class QueueUrl extends NoToString {
    public abstract String getUrl();

    public QueueName getQueueName() {
        String temp = getUrl();

        int lastSlashIndex = temp.lastIndexOf('/');

        if (lastSlashIndex == -1) {
            throw new RuntimeException(getUrl() + " is not a valid SQS queue URL because it contains no forward slash");
        }

        int startNameIndex = lastSlashIndex + 1;

        if (temp.length() < startNameIndex) {
            throw new RuntimeException(getUrl() + " is not a valid SQS queue URL because it ends with a forward slash");
        }

        return ImmutableQueueName.builder().name(temp.substring(startNameIndex)).build();
    }
}

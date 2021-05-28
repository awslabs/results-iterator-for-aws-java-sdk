package com.awslabs.dynamodb.implementations;

import com.awslabs.dynamodb.data.TableName;
import com.awslabs.dynamodb.interfaces.DynamoDbHelper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import javax.inject.Inject;

public class BasicDynamoDbHelper implements DynamoDbHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicDynamoDbHelper.class);
    @Inject
    DynamoDbClient dynamoDbClient;

    @Inject
    public BasicDynamoDbHelper() {
    }

    @Override
    public Try<TableDescription> tryDescribeTable(TableName tableName) {
        DescribeTableRequest describeTableRequest = DescribeTableRequest.builder()
                .tableName(tableName.getTableName())
                .build();

        return Try.of(() -> dynamoDbClient.describeTable(describeTableRequest))
                .map(DescribeTableResponse::table);
    }
}

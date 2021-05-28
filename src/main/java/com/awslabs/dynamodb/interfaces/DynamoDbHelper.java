package com.awslabs.dynamodb.interfaces;

import com.awslabs.dynamodb.data.TableName;
import io.vavr.control.Try;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

public interface DynamoDbHelper {
    Try<TableDescription> tryDescribeTable(TableName tableName);
}

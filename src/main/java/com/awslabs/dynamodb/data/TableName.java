package com.awslabs.dynamodb.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class TableName extends NoToString {
    public abstract String getTableName();
}

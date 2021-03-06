package com.awslabs.sqs.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class MaxNumberOfMessages extends NoToString {
    public abstract Integer getValue();
}

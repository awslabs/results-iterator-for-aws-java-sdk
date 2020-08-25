package com.awslabs.sqs.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.time.Duration;

@Gson.TypeAdapters
@Value.Immutable
public abstract class VisibilityTimeout extends NoToString {
    public abstract Duration getDuration();
}

package com.awslabs.lambda.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class FunctionAlias extends NoToString {
    public abstract String getAlias();
}

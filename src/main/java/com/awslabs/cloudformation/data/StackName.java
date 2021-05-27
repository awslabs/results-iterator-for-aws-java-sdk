package com.awslabs.cloudformation.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class StackName extends NoToString {
    public abstract String getStackName();
}

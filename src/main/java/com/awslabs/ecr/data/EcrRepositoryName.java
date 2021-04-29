package com.awslabs.ecr.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class EcrRepositoryName extends NoToString {
    public abstract String getName();
}

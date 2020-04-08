package com.awslabs.iam.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class AssumeRolePolicyDocument extends NoToString {
    public abstract String getDocument();
}

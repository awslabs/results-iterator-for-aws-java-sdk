package com.awslabs.lambda.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.io.File;

@Gson.TypeAdapters
@Value.Immutable
public abstract class JavaLambdaFunctionDirectory extends NoToString {
    public abstract File getDirectory();
}

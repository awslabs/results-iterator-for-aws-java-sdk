package com.awslabs.s3.helpers.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
/**
 * S3Path is an S3 key value that does not include the filename
 */
public abstract class S3Path extends NoToString {
    public abstract String path();
}

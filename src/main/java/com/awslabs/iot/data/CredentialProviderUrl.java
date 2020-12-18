package com.awslabs.iot.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class CredentialProviderUrl extends NoToString {
    public abstract String getCredentialProviderUrl();
}

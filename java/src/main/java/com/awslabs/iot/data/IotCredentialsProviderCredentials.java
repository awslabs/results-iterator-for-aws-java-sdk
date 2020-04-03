package com.awslabs.iot.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class IotCredentialsProviderCredentials {
    public abstract ImmutableSessionCredentials getCredentials();
}

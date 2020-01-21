package com.awslabs.aws.iot.resultsiterator.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class ClientName {
    public abstract String getClientName();
}

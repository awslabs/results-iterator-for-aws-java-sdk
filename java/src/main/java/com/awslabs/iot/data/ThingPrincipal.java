package com.awslabs.iot.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class ThingPrincipal {
    public abstract String getPrincipal();

    @Override
    public String toString() {
        // This is to make sure string concatenation with this type throws an exception immediately
        return null;
    }
}

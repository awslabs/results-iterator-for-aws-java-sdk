package com.awslabs.iot.data;

import com.awslabs.data.NoToString;
import io.vavr.collection.List;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class Statement extends NoToString {
    public abstract Effect getEffect();

    public abstract List<String> getAction();

    public abstract List<String> getResource();
}

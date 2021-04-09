package com.awslabs.iot.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;

@Gson.TypeAdapters
@Value.Immutable
public abstract class Statement extends NoToString {
    public abstract Effect getEffect();

    public abstract List<String> getAction();

    public abstract List<String> getResource();
}

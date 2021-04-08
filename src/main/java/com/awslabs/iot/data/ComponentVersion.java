package com.awslabs.iot.data;

import com.awslabs.data.NoToString;
import com.vdurmont.semver4j.Semver;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class ComponentVersion extends NoToString {
    public abstract Semver getVersion();
}

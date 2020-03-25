package com.awslabs.general.helpers.data;

import com.awslabs.data.NoToString;
import io.vavr.collection.List;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class ProcessOutput extends NoToString {
    public abstract int getExitCode();

    public abstract List<String> getStandardOutStrings();

    public abstract List<String> getStandardErrorStrings();
}

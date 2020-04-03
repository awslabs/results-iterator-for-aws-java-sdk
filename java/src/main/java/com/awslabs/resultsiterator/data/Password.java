package com.awslabs.resultsiterator.data;

import com.awslabs.data.NoToString;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class Password extends NoToString {
    public static final char[] BLANK_PASSWORD = "".toCharArray();

    @Value.Default
    public char[] getPassword() {
        return BLANK_PASSWORD;
    }
}

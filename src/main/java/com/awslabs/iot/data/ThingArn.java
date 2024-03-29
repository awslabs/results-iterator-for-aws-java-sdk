package com.awslabs.iot.data;

import com.awslabs.data.NoToString;
import com.google.common.base.Preconditions;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class ThingArn extends NoToString {
    public abstract String getArn();

    @Value.Check
    protected void check() {
        Preconditions.checkState(getArn().contains(":thing/"),
                "Supplied ARN does not appear to be a thing ARN [" + getArn() + "]");
    }
}

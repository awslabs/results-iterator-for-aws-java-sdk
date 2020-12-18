package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.ImmutableThingName;
import com.awslabs.iot.data.ThingArn;
import com.awslabs.iot.data.ThingName;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;

import javax.inject.Inject;

public class BasicIotIdExtractor implements IotIdExtractor {
    @Inject
    public BasicIotIdExtractor() {
    }

    @Override
    public ThingName extractThingName(ThingArn thingArn) {
        return ImmutableThingName.builder()
                .name(thingArn.getArn().substring(thingArn.getArn().lastIndexOf('/') + 1))
                .build();
    }
}

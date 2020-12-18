package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iot.data.ThingArn;
import com.awslabs.iot.data.ThingName;

public interface IotIdExtractor {
    ThingName extractThingName(ThingArn thingArn);
}

package com.awslabs.iot.helpers.interfaces;

import com.awslabs.iot.data.ThingArn;
import com.awslabs.iot.data.ThingGroup;
import com.awslabs.iot.data.ThingGroupArn;
import com.awslabs.iot.data.ThingName;
import io.vavr.control.Either;
import io.vavr.control.Try;

public interface IotIdExtractor {
    ThingName extractThingName(ThingArn thingArn);

    ThingGroup extractThingGroup(ThingGroupArn thingGroupArn);

    Try<Either<ThingName, ThingGroup>> tryExtractThingNameOrThingGroup(String arn);
}

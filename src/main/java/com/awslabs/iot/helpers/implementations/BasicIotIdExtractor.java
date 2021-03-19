package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;
import io.vavr.control.Either;
import io.vavr.control.Try;

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

    @Override
    public ThingGroup extractThingGroup(ThingGroupArn thingGroupArn) {
        return ImmutableThingGroup.builder()
                .name(thingGroupArn.getArn().substring(thingGroupArn.getArn().lastIndexOf('/') + 1))
                .build();
    }

    @Override
    public Try<Either<ThingName, ThingGroup>> tryExtractThingNameOrThingGroup(String arn) {
        Try<Either<ThingArn, ThingGroupArn>> tryEither = Try.of(() -> Either.<ThingArn, ThingGroupArn>left(ImmutableThingArn.builder().arn(arn).build()))
                .orElse(() -> Try.of(() -> Either.right(ImmutableThingGroupArn.builder().arn(arn).build())));

        if (tryEither.isFailure()) {
            return Try.failure(new RuntimeException("The supplied ARN [" + arn + "] is neither a thing ARN or a thing group ARN"));
        }

        return Try.of(() -> tryEither.get()
                .mapLeft(this::extractThingName)
                .map(this::extractThingGroup));
    }
}

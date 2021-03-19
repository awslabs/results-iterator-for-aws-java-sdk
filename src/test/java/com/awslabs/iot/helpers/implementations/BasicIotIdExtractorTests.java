package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.*;
import com.awslabs.iot.helpers.interfaces.IotIdExtractor;
import com.awslabs.resultsiterator.implementations.DaggerTestInjector;
import com.awslabs.resultsiterator.implementations.TestInjector;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class BasicIotIdExtractorTests {
    private static final String FAKE = "fake";
    private static final String fakeThingArnString = "arn:aws:iot:us-east-1:999999999999:thing/" + FAKE;
    private static final ThingArn fakeThingArn = ImmutableThingArn.builder().arn(fakeThingArnString).build();
    private static final String fakeThingGroupArnString = "arn:aws:iot:us-east-1:999999999999:thinggroup/" + FAKE;
    private static final ThingGroupArn fakeThingGroupArn = ImmutableThingGroupArn.builder().arn(fakeThingGroupArnString).build();
    private IotIdExtractor iotIdExtractor;

    @Before
    public void setup() {
        TestInjector injector = DaggerTestInjector.create();
        iotIdExtractor = injector.iotIdExtractor();
    }

    @Test
    public void shouldExtractThingName() {
        ThingName thingName = iotIdExtractor.extractThingName(fakeThingArn);

        assertThat(thingName.getName(), is(FAKE));
    }

    @Test
    public void shouldExtractThingGroup() {
        ThingGroup thingGroup = iotIdExtractor.extractThingGroup(fakeThingGroupArn);

        assertThat(thingGroup.getName(), is(FAKE));
    }


    @Test
    public void shouldExtractThingNameUsingEither() {
        Try<Either<ThingName, ThingGroup>> tryEither = iotIdExtractor.tryExtractThingNameOrThingGroup(fakeThingArn.getArn());

        assertThat(tryEither.isSuccess(), is(true));

        ThingName thingName = tryEither.get().getLeft();

        assertThat(thingName.getName(), is(FAKE));
    }

    @Test
    public void shouldExtractThingGroupUsingEither() {
        Try<Either<ThingName, ThingGroup>> tryEither = iotIdExtractor.tryExtractThingNameOrThingGroup(fakeThingGroupArn.getArn());

        assertThat(tryEither.isSuccess(), is(true));

        ThingGroup thingGroup = tryEither.get().get();

        assertThat(thingGroup.getName(), is(FAKE));
    }

    @Test
    public void shouldNotExtractValueUsingEitherOnBadInput() {
        Try<Either<ThingName, ThingGroup>> tryEither = iotIdExtractor.tryExtractThingNameOrThingGroup("this is not an ARN");

        assertThat(tryEither.isFailure(), is(true));
    }

    @Test
    public void shouldNotCreateThingArnWithInvalidArn() {
        assertThrows(RuntimeException.class, () -> ImmutableThingArn.builder().arn(fakeThingGroupArnString).build());
    }

    @Test
    public void shouldNotCreateThingGroupArnWithInvalidArn() {
        assertThrows(RuntimeException.class, () -> ImmutableThingGroupArn.builder().arn(fakeThingArnString).build());
    }
}

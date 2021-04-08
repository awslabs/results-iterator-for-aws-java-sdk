package com.awslabs.iot.helpers.implementations;

import com.awslabs.iot.data.ThingPrincipal;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.regions.Region;

import java.util.function.Predicate;

public class ArnHelper {
    public enum ArnType {
        IOT_CERT("cert");

        private final String typeString;

        ArnType(String typeString) {
            this.typeString = typeString;
        }

        public static Option<ArnType> valueOfArnTypeString(String arnTypeString) {
            return Stream.of(values())
                    .find(arnType -> arnType.typeString.equals(arnTypeString));
        }
    }

    public static final String ARN_AWS_IOT = "arn:aws:iot";

    public static Predicate<String> isIotArn() {
        return string -> string.startsWith(ARN_AWS_IOT);
    }

    public static Predicate<String> containsValidRegion() {
        return string -> Stream.ofAll(Region.regions())
                .map(Region::id)
                .find(region -> region.equals(getRegionStringFromArn(string).getOrNull()))
                .isDefined();
    }

    @NotNull
    public static Option<String> getRegionStringFromArn(String arn) {
        // Split the ARN on colons
        return Try.of(() -> splitOnColons(arn)
                // Get the fourth (zero-based) segment which should be the region
                .get(3))
                // If anything fails, return none
                .toOption();
    }

    @NotNull
    public static Option<Region> getRegionFromArn(String arn) {
        Option<String> regionStringOption = getRegionStringFromArn(arn);

        if (regionStringOption.isEmpty()) {
            return Option.none();
        }

        return Stream.ofAll(Region.regions())
                .find(region -> region.id().equals(regionStringOption.get()));
    }

    public static Option<String> getIotArnTypeString(String arn) {
        // Split the ARN on colons and only return a value if it is valid
        return getValidArnSplitOnColons(arn)
                // Get the last element which should be contain the type (cert, cacert, etc), a slash, and the ID
                .lastOption()
                // Split the final ARN element on slashes and only return a value if it is valid
                .map(ArnHelper::getValidFinalArnElementSplitOnSlashes)
                // Get the first element
                .map(Traversable::get);
    }

    public static Option<ArnType> getIotArnType(String arn) {
        return getIotArnTypeString(arn)
                .flatMap(ArnType::valueOfArnTypeString);
    }

    /**
     * Split an ARN string on colons
     *
     * @param arn a string that is possibly an ARN
     * @return Some(String) if the ARN contains exactly 6 elements after splitting, None otherwise
     */
    private static Stream<String> getValidArnSplitOnColons(String arn) {
        Stream<String> split = splitOnColons(arn);

        // Valid ARNs have 6 components
        if (split.size() != 6) {
            return Stream.empty();
        }

        return split;
    }

    private static Stream<String> getValidFinalArnElementSplitOnSlashes(String lastArnElement) {
        Stream<String> split = splitOnSlashes(lastArnElement);

        // Valid final elements have 2 components
        if (split.size() != 2) {
            return Stream.empty();
        }

        return split;
    }

    private static Stream<String> splitOnSlashes(String string) {
        return Stream.of(string.split("/"));
    }

    private static Stream<String> splitOnColons(String arn) {
        return Stream.of(arn.split(":"));
    }

    public static Predicate<ThingPrincipal> isCertificate() {
        return thingPrincipal -> thingPrincipal.getPrincipal().isEmpty();
//        .startsWith("arn:aws:iot:")
//                arn:aws:iot:us-east-1:541589084637:cert/51b46a1f1ffa329b4a3914ff8c775fcab80b6084ec7649c1509f5fa5474af622
//        return null;
    }
}

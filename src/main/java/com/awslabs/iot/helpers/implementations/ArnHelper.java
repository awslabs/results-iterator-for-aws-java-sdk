package com.awslabs.iot.helpers.implementations;

import com.awslabs.data.NoToString;
import com.awslabs.iam.data.IamUser;
import com.awslabs.iam.data.ImmutableIamUser;
import com.awslabs.iot.data.CertificateArn;
import com.awslabs.iot.data.ImmutableCertificateArn;
import com.awslabs.iot.data.RoleAlias;
import com.awslabs.iot.data.ThingPrincipal;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.regions.Region;

import java.util.function.Predicate;

public class ArnHelper {
    public enum ArnType {
        IOT_CERT("cert", CertificateArn.class),
        IAM_USER("user", IamUser.class),
        IOT_ROLE_ALIAS("rolealias", RoleAlias.class);

        private final String typeString;

        private final Class<? extends NoToString> typeSafeClass;

        ArnType(String typeString, Class<? extends NoToString> typeSafeClass) {
            this.typeString = typeString;
            this.typeSafeClass = typeSafeClass;
        }

        public static Option<ArnType> valueOfArnTypeString(String arnTypeString) {
            return Stream.of(values())
                    .find(arnType -> arnType.typeString.equals(arnTypeString));
        }

        public Class<? extends NoToString> getTypeSafeClass() {
            return typeSafeClass;
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

    public static Option<String> getArnTypeString(String arn) {
        // Split the ARN on colons and only return a value if it is valid
        return getValidArnSplitOnColons(arn)
                // Get the last element which should be contain the type (cert, cacert, etc), a slash, and the ID
                .lastOption()
                // Split the final ARN element on slashes and only return a value if it is valid
                .map(ArnHelper::getValidFinalArnElementSplitOnSlashes)
                // Get the first element
                .map(Traversable::get);
    }

    public static Option<ArnType> getArnType(String arn) {
        return getArnTypeString(arn)
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

    public static Stream<Tuple2<String, ? extends Class<? extends NoToString>>> getArnStringAndTypeSafeClasses(Stream<ThingPrincipal> thingPrincipalStream) {
        return thingPrincipalStream
                .flatMap(ArnHelper::getArnStringAndTypeSafeClass);
    }

    public static Option<Tuple2<String, ? extends Class<? extends NoToString>>> getArnStringAndTypeSafeClass(ThingPrincipal thingPrincipal) {
        return Option.of(thingPrincipal)
                .map(ThingPrincipal::getPrincipal)
                .map(principalArn -> Tuple.of(principalArn, getArnType(principalArn)))
                .filter(tuple -> tuple._2.isDefined())
                .map(tuple -> Tuple.of(tuple._1, tuple._2.get().getTypeSafeClass()));
    }

    public static Predicate<ThingPrincipal> isIotCertificate() {
        return thingPrincipal -> getArnType(thingPrincipal.getPrincipal())
                // Make sure the type safe version of this is a CertificateArn
                .filter(arnType -> arnType.getTypeSafeClass().isAssignableFrom(CertificateArn.class))
                .isDefined();
    }

    public static Predicate<ThingPrincipal> isIamUser() {
        return thingPrincipal -> getArnType(thingPrincipal.getPrincipal())
                // Make sure the type safe version of this is an IamUser
                .filter(arnType -> arnType.getTypeSafeClass().isAssignableFrom(IamUser.class))
                .isDefined();
    }

    public static Predicate<String> isRoleAlias() {
        return string -> getArnType(string)
                // Make sure the type safe version of this is a RoleAlias
                .filter(arnType -> arnType.getTypeSafeClass().isAssignableFrom(RoleAlias.class))
                .isDefined();
    }

    public static Stream<CertificateArn> getCertificateArnsFromThingPrincipals(Stream<ThingPrincipal> thingPrincipalStream) {
        return thingPrincipalStream
                .flatMap(ArnHelper::getCertificateArnFromThingPrincipal);
    }

    public static Option<CertificateArn> getCertificateArnFromThingPrincipal(ThingPrincipal thingPrincipal) {
        return Option.of(thingPrincipal)
                .filter(value -> isIotCertificate().test(value))
                .map(value -> ImmutableCertificateArn.builder().arn(value.getPrincipal()).build());
    }

    public static Stream<IamUser> getIamUsersFromThingPrincipals(Stream<ThingPrincipal> thingPrincipalStream) {
        return thingPrincipalStream
                .flatMap(ArnHelper::getIamUserFromThingPrincipal);
    }

    public static Option<IamUser> getIamUserFromThingPrincipal(ThingPrincipal thingPrincipal) {
        return Option.of(thingPrincipal)
                .filter(value -> isIamUser().test(value))
                .map(value -> ImmutableIamUser.builder().arn(value.getPrincipal()).build());
    }
}

package com.awslabs.general.helpers.implementations;

import com.awslabs.iam.data.IamUser;
import com.awslabs.iot.data.CertificateArn;
import com.awslabs.iot.data.ImmutableThingPrincipal;
import com.awslabs.iot.data.ThingPrincipal;
import com.awslabs.iot.helpers.implementations.ArnHelper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;

import static com.awslabs.iot.helpers.implementations.ArnHelper.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArnHelperTest {
    public static final String VALID_US_EAST_1_ROLE_ALIAS_STRING = "arn:aws:iot:us-east-1:123451234512:rolealias/somealiasedrole";
    public static final String VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING = "arn:aws:iot:us-east-1:123451234512:cert/51b46a1f1ffa329b4a3914ff8c775fcab80b6084ec7649c1509f5fa5474af622";
    public static final ThingPrincipal VALID_US_EAST_1_IOT_THING_PRINCIPAL = ImmutableThingPrincipal.builder().principal(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING).build();
    public static final String INVALID_IOT_CERTIFICATE_ARN_STRING_1 = "arn:aws:iot:us-eas";
    public static final ThingPrincipal INVALID_IOT_THING_PRINCIPAL_1 = ImmutableThingPrincipal.builder().principal(INVALID_IOT_CERTIFICATE_ARN_STRING_1).build();
    public static final String INVALID_IOT_CERTIFICATE_ARN_STRING_2 = "arn:aws:iot:us-east-9:123451234512:cert/51b46a1f1ffa329b4a3914ff8c775fcab80b6084ec7649c1509f5fa5474af622";
    public static final String VALID_IAM_USER_ARN_STRING = "arn:aws:iam::123451234512:user/somedeveloper";
    public static final ThingPrincipal VALID_IAM_USER_THING_PRINCIPAL = ImmutableThingPrincipal.builder().principal(VALID_IAM_USER_ARN_STRING).build();
    public static final List<ThingPrincipal> thingPrincipalList = List.of(VALID_US_EAST_1_IOT_THING_PRINCIPAL, INVALID_IOT_THING_PRINCIPAL_1, VALID_IAM_USER_THING_PRINCIPAL);

    @Test
    public void shouldReturnThatAllIotArnsAreIotArns() {
        assertThat(isIotArn().test(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING), is(true));
        assertThat(isIotArn().test(INVALID_IOT_CERTIFICATE_ARN_STRING_1), is(true));
        assertThat(isIotArn().test(INVALID_IOT_CERTIFICATE_ARN_STRING_2), is(true));
        assertThat(isIotArn().test(VALID_US_EAST_1_ROLE_ALIAS_STRING), is(true));
    }

    @Test
    public void shouldGetCorrectRegionStringFromValidIotCertificateArn() {
        Option<String> regionStringOption = getRegionStringFromArn(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING);
        assertThat(regionStringOption.isDefined(), is(true));
        assertThat(regionStringOption.get(), is(Region.US_EAST_1.id()));
    }

    @Test
    public void shouldGetCorrectRegionFromValidIotCertificateArn() {
        Option<Region> regionOption = getRegionFromArn(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING);
        assertThat(regionOption.isDefined(), is(true));
        assertThat(regionOption.get(), is(Region.US_EAST_1));
    }

    @Test
    public void shouldReturnThatRegionIsValidFromValidIotCertificateArn() {
        assertThat(containsValidRegion().test(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING), is(true));
    }

    @Test
    public void shouldReturnThatRegionIsInvalidFromInvalidIotCertificateArn1() {
        assertThat(containsValidRegion().test(INVALID_IOT_CERTIFICATE_ARN_STRING_1), is(false));
    }

    @Test
    public void shouldReturnThatRegionIsInvalidFromInvalidIotCertificateArn2() {
        assertThat(containsValidRegion().test(INVALID_IOT_CERTIFICATE_ARN_STRING_2), is(false));
    }

    @Test
    public void shouldNotReturnTruncatedTypeStringInTruncatedArn() {
        assertThat(getArnTypeString(INVALID_IOT_CERTIFICATE_ARN_STRING_1), is(Option.none()));
    }

    @Test
    public void shouldReturnCorrectArnTypeOnValidCertificateArnString() {
        assertThat(ArnHelper.getArnType(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING), is(Option.some(ArnType.IOT_CERT)));
    }

    @Test
    public void shouldReturnCorrectArnTypeOnValidRoleAliasArnString() {
        assertThat(getArnType(VALID_US_EAST_1_ROLE_ALIAS_STRING), is(Option.some(ArnType.IOT_ROLE_ALIAS)));
    }

    @Test
    public void shouldReturnNoneOnInvalidCertificateArn() {
        assertThat(ArnHelper.getArnType(INVALID_IOT_CERTIFICATE_ARN_STRING_1), is(Option.none()));
    }

    @Test
    public void shouldBeAnIotCertificateOnValidCertificateArnInThingPrincipal() {
        assertThat(isIotCertificate().test(VALID_US_EAST_1_IOT_THING_PRINCIPAL), is(true));
    }

    @Test
    public void shouldNotBeAnIamUserOnValidCertificateArnInThingPrincipal() {
        assertThat(isIamUser().test(VALID_US_EAST_1_IOT_THING_PRINCIPAL), is(false));
    }

    @Test
    public void shouldNotBeAnIotCertificateOnInvalidCertificateArnInThingPrincipal() {
        assertThat(isIotCertificate().test(INVALID_IOT_THING_PRINCIPAL_1), is(false));
    }

    @Test
    public void shouldBeAnIamUserOnValidIamUserArnInThingPrincipal() {
        assertThat(isIamUser().test(VALID_IAM_USER_THING_PRINCIPAL), is(true));
    }

    @Test
    public void shouldNotBeAnIotCertificateOnValidIamUserArnInThingPrincipal() {
        assertThat(isIotCertificate().test(VALID_IAM_USER_THING_PRINCIPAL), is(false));
    }

    @Test
    public void shouldBeARoleAliasOnValidRoleAliasArnString() {
        assertThat(isRoleAlias().test(VALID_US_EAST_1_ROLE_ALIAS_STRING), is(true));
    }

    @Test
    public void shouldNotBeARoleAliasOnValidIamUserArnString() {
        assertThat(isRoleAlias().test(VALID_IAM_USER_ARN_STRING), is(false));
    }

    @Test
    public void shouldFilterCertificateArnsFromStreamOfThingPrincipals() {
        List<CertificateArn> certificateArnList = getCertificateArnsFromThingPrincipals(thingPrincipalList.toStream()).toList();

        assertThat(certificateArnList.length(), is(1));
        assertThat(certificateArnList.get().getArn(), is(VALID_US_EAST_1_IOT_CERTIFICATE_ARN_STRING));
    }

    @Test
    public void shouldFilterIamUsersFromStreamOfThingPrincipals() {
        List<IamUser> iamUserList = getIamUsersFromThingPrincipals(thingPrincipalList.toStream()).toList();

        assertThat(iamUserList.length(), is(1));
        assertThat(iamUserList.get().getArn(), is(VALID_IAM_USER_ARN_STRING));
    }
}

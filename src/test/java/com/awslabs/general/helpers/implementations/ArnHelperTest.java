package com.awslabs.general.helpers.implementations;

import com.awslabs.iot.helpers.implementations.ArnHelper;
import io.vavr.control.Option;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;

import static com.awslabs.iot.helpers.implementations.ArnHelper.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArnHelperTest {

    public static final String VALID_US_EAST_1_IOT_CERTIFICATE_ARN = "arn:aws:iot:us-east-1:123451234512:cert/51b46a1f1ffa329b4a3914ff8c775fcab80b6084ec7649c1509f5fa5474af622";
    public static final String INVALID_IOT_CERTIFICATE_ARN_1 = "arn:aws:iot:us-eas";
    public static final String INVALID_IOT_CERTIFICATE_ARN_2 = "arn:aws:iot:us-east-9:123451234512:cert/51b46a1f1ffa329b4a3914ff8c775fcab80b6084ec7649c1509f5fa5474af622";

    @Test
    public void shouldReturnThatAllIotArnsAreIotArns() {
        assertThat(isIotArn().test(VALID_US_EAST_1_IOT_CERTIFICATE_ARN), is(true));
        assertThat(isIotArn().test(INVALID_IOT_CERTIFICATE_ARN_1), is(true));
        assertThat(isIotArn().test(INVALID_IOT_CERTIFICATE_ARN_2), is(true));
    }

    @Test
    public void shouldGetCorrectRegionStringFromValidIotCertificateArn() {
        Option<String> regionStringOption = getRegionStringFromArn(VALID_US_EAST_1_IOT_CERTIFICATE_ARN);
        assertThat(regionStringOption.isDefined(), is(true));
        assertThat(regionStringOption.get(), is(Region.US_EAST_1.id()));
    }

    @Test
    public void shouldGetCorrectRegionFromValidIotCertificateArn() {
        Option<Region> regionOption = getRegionFromArn(VALID_US_EAST_1_IOT_CERTIFICATE_ARN);
        assertThat(regionOption.isDefined(), is(true));
        assertThat(regionOption.get(), is(Region.US_EAST_1));
    }

    @Test
    public void shouldReturnThatRegionIsValidFromValidIotCertificateArn() {
        assertThat(containsValidRegion().test(VALID_US_EAST_1_IOT_CERTIFICATE_ARN), is(true));
    }

    @Test
    public void shouldReturnThatRegionIsInvalidFromInvalidIotCertificateArn1() {
        assertThat(containsValidRegion().test(INVALID_IOT_CERTIFICATE_ARN_1), is(false));
    }

    @Test
    public void shouldReturnThatRegionIsInvalidFromInvalidIotCertificateArn2() {
        assertThat(containsValidRegion().test(INVALID_IOT_CERTIFICATE_ARN_2), is(false));
    }

    @Test
    public void shouldNotReturnTruncatedTypeStringInTruncatedArn() {
        assertThat(getIotArnTypeString(INVALID_IOT_CERTIFICATE_ARN_1), is(Option.none()));
    }

    @Test
    public void shouldReturnCorrectArnTypeOnValidCertificateArn() {
        assertThat(ArnHelper.getIotArnType(VALID_US_EAST_1_IOT_CERTIFICATE_ARN), is(Option.some(ArnType.IOT_CERT)));
    }

    @Test
    public void shouldReturnNoneOnInvalidCertificateArn() {
        assertThat(ArnHelper.getIotArnType(INVALID_IOT_CERTIFICATE_ARN_1), is(Option.none()));
    }
}

package com.awslabs.general.helpers.implementations;

import io.vavr.control.Try;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import software.amazon.awssdk.core.SdkSystemSetting;

import static com.awslabs.general.helpers.implementations.IoHelper.getDefaultHttpClient;

public class AwsHelper {
    public static boolean isEc2() {
        String metadataUrl = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getStringValueOrThrow();

        HttpClient client = getDefaultHttpClient();
        HttpGet httpGet = new HttpGet(metadataUrl);

        return Try.of(() -> client.execute(httpGet))
                .map(HttpResponse::getStatusLine)
                .map(StatusLine::getStatusCode)
                // Did we reach the metadata service and get a successful response?
                .map(statusCode -> statusCode == 200)
                // If we weren't successful return false
                .getOrElse(false);
    }
}

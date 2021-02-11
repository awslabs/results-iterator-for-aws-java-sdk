package com.awslabs.general.helpers.implementations;

import com.awslabs.general.helpers.interfaces.AwsHelper;
import com.awslabs.general.helpers.interfaces.IoHelper;
import io.vavr.control.Try;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import software.amazon.awssdk.core.SdkSystemSetting;

import javax.inject.Inject;

public class BasicAwsHelper implements AwsHelper {
    @Inject
    IoHelper ioHelper;

    @Inject
    public BasicAwsHelper() {
    }

    @Override
    public boolean isEc2() {
        String metadataUrl = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getStringValueOrThrow();

        HttpClient client = ioHelper.getDefaultHttpClient();
        HttpGet httpGet = new HttpGet(metadataUrl);

        Try<Integer> tryHttpGetStatusCode = Try.of(() -> client.execute(httpGet))
                .map(HttpResponse::getStatusLine)
                .map(StatusLine::getStatusCode);

        if (tryHttpGetStatusCode.isFailure()) {
            return false;
        }

        // Did we reach the metadata service and get a successful response?
        return (tryHttpGetStatusCode.get() == 200);
    }
}

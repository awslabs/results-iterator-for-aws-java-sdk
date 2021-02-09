package com.awslabs.general.helpers.implementations;

import com.amazonaws.util.EC2MetadataUtils;
import com.awslabs.general.helpers.interfaces.AwsHelper;
import com.awslabs.general.helpers.interfaces.IoHelper;
import io.vavr.control.Try;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import javax.inject.Inject;

public class BasicAwsHelper implements AwsHelper {
    @Inject
    IoHelper ioHelper;

    @Inject
    public BasicAwsHelper() {
    }

    @Override
    public boolean isEc2() {
        String metadataUrl = EC2MetadataUtils.getHostAddressForEC2MetadataService();

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

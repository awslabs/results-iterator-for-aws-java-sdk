package com.awslabs.general.helpers.implementations;

import com.amazonaws.util.EC2MetadataUtils;
import com.awslabs.general.helpers.interfaces.AwsHelper;
import io.vavr.control.Try;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class BasicAwsHelper implements AwsHelper {
    @Override
    public boolean isEc2() {
        String metadataUrl = EC2MetadataUtils.getHostAddressForEC2MetadataService();

        // Set some short timeouts so this doesn't hang while testing
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(100);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(100);
        requestBuilder = requestBuilder.setSocketTimeout(100);

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        HttpClient client = builder.build();
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

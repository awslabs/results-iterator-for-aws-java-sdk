package com.awslabs.iot.data;

import org.immutables.gson.Gson;
import org.immutables.value.Value;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

@Gson.TypeAdapters
@Value.Immutable
public abstract class SessionCredentials {
    public abstract String getAccessKeyId();

    public abstract String getSecretAccessKey();

    public abstract String getSessionToken();

    public AwsSessionCredentials toAwsSessionCredentials() {
        return AwsSessionCredentials.create(getAccessKeyId(), getSecretAccessKey(), getSessionToken());
    }
}

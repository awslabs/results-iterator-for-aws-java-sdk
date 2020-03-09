package com.awslabs.iot.data;

public enum V2IotEndpointType {
    DATA("iot:Data"),
    DATA_ATS("iot:Data-ATS"),
    CREDENTIAL_PROVIDER("iot:CredentialProvider"),
    JOBS("iot:Jobs");

    private final String value;

    V2IotEndpointType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

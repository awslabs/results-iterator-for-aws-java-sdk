package com.awslabs.aws.iot.resultsiterator.helpers.interfaces;

public interface JsonHelper {
    String toJson(Object object);

    <T> T fromJson(Class<T> clazz, byte[] json);
}

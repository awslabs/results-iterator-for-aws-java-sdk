package com.awslabs.general.helpers.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Lazy;
import io.vavr.control.Try;
import io.vavr.jackson.datatype.VavrModule;

public class JacksonHelper {
    private static final Lazy<ObjectMapper> lazyObjectMapper = Lazy.of(JacksonHelper::getObjectMapper);

    public static Try<String> toJsonString(Object object) {
        return Try.of(() -> lazyObjectMapper.get().writeValueAsString(object));
    }

    public static Try<byte[]> toJsonBytes(Object object) {
        return Try.of(() -> lazyObjectMapper.get().writeValueAsBytes(object));
    }

    private static ObjectMapper getObjectMapper() {
        return new ObjectMapper().registerModule(new VavrModule());
    }
}

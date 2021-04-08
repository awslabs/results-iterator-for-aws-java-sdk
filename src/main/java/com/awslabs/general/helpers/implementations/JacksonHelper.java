package com.awslabs.general.helpers.implementations;

import com.fasterxml.jackson.databind.JsonNode;
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

    public static <T extends JsonNode> Try<T> toJsonNode(Object object) {
        return Try.of(() -> lazyObjectMapper.get().valueToTree(object));
    }

    private static ObjectMapper getObjectMapper() {
        // Get an object mapper that also is able to serialize vavr objects
        return new ObjectMapper().registerModule(new VavrModule());
    }
}

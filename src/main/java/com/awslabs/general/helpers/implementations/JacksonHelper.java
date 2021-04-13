package com.awslabs.general.helpers.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vavr.Lazy;
import io.vavr.control.Try;
import io.vavr.jackson.datatype.VavrModule;

public class JacksonHelper {
    private static final Lazy<ObjectMapper> lazyObjectMapper = Lazy.of(JacksonHelper::getObjectMapper);
    private static final Lazy<ObjectMapper> lazyYamlObjectMapper = Lazy.of(JacksonHelper::getYamlObjectMapper);

    public static Try<String> toJsonString(Object object) {
        return Try.of(() -> lazyObjectMapper.get().writeValueAsString(object));
    }

    public static Try<String> toYamlString(Object object) {
        return Try.of(() -> lazyYamlObjectMapper.get().writeValueAsString(object));
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

    private static ObjectMapper getYamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory()).registerModule(new VavrModule());
    }
}

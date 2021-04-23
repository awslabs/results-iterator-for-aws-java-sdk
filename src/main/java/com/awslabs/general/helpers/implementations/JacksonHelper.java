package com.awslabs.general.helpers.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vavr.Lazy;
import io.vavr.control.Try;
import io.vavr.jackson.datatype.VavrModule;

public class JacksonHelper {
    private static final Lazy<ObjectMapper> lazyJsonObjectMapper = Lazy.of(JacksonHelper::getObjectMapper);
    private static final Lazy<ObjectMapper> lazyYamlObjectMapper = Lazy.of(JacksonHelper::getYamlObjectMapper);

    public static Try<String> tryToJsonString(Object object) {
        return Try.of(() -> lazyJsonObjectMapper.get().writeValueAsString(object));
    }

    public static Try<String> tryToYamlString(Object object) {
        return Try.of(() -> lazyYamlObjectMapper.get().writeValueAsString(object));
    }

    public static Try<byte[]> tryToJsonBytes(Object object) {
        return Try.of(() -> lazyJsonObjectMapper.get().writeValueAsBytes(object));
    }

    public static <T extends JsonNode> Try<T> tryToJsonNode(Object object) {
        return Try.of(() -> lazyJsonObjectMapper.get().valueToTree(object));
    }

    public static <T> Try<T> tryParseJson(String json, Class<T> clazz) {
        return Try.of(() -> lazyJsonObjectMapper.get().readValue(json, clazz));
    }

    public static <T> Try<T> tryParseYaml(String yaml, Class<T> clazz) {
        return Try.of(() -> lazyYamlObjectMapper.get().readValue(yaml, clazz));
    }

    private static ObjectMapper getObjectMapper() {
        // Get an object mapper that also is able to serialize vavr objects
        return new ObjectMapper().registerModule(new VavrModule());
    }

    private static ObjectMapper getYamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory()).registerModule(new VavrModule());
    }
}

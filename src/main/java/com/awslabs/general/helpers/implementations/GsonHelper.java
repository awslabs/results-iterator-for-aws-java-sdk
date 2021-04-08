package com.awslabs.general.helpers.implementations;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import io.vavr.Lazy;

import java.util.ServiceLoader;

public class GsonHelper {
    private static final Lazy<GsonBuilder> lazyGsonBuilder = Lazy.of(GsonHelper::getGsonBuilder);

    public static String toJson(Object object) {
        return lazyGsonBuilder.get()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(object);
    }

    private static GsonBuilder getGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        ServiceLoader.load(TypeAdapterFactory.class)
                .forEach(gsonBuilder::registerTypeAdapterFactory);

        return gsonBuilder;
    }

    public static <T> T fromJson(Class<T> clazz, byte[] json) {
        return lazyGsonBuilder.get()
                .create()
                .fromJson(new String(json), clazz);
    }
}

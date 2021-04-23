package com.awslabs.general.helpers.implementations;

import io.vavr.control.Try;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class StreamHelper {
    public static String inputStreamToString(InputStream inputStream) {
        return Try.withResources(() -> new Scanner(inputStream, StandardCharsets.UTF_8.name()))
                .of(scanner -> scanner.useDelimiter("\\A"))
                .map(Scanner::next)
                .get();
    }
}

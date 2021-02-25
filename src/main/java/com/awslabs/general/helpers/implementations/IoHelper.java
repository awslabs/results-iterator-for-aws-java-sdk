package com.awslabs.general.helpers.implementations;

import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IoHelper {
    public static void writeFile(String filename, String contents) {
        Try.withResources(() -> new PrintWriter(filename))
                .of(printWriter -> Try.run(() -> printWriter.print(contents)));
    }

    public static String readFile(String filename) {
        return Try.of(() -> Files.readAllBytes(Paths.get(filename)))
                .map(bytes -> new String(bytes, Charset.defaultCharset()))
                .get();
    }


    public static void download(String urlString, String outputFilename) {
        // From: http://stackoverflow.com/a/921400
        Try.of(() -> new URL(urlString))
                .mapTry(URL::openStream)
                .map(Channels::newChannel)
                .mapTry(readableByteChannel -> new FileOutputStream(outputFilename).getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE))
                .get();
    }

    public static boolean exists(String filename) {
        return Paths.get(filename).toFile().exists();
    }

    public static HttpClient getDefaultHttpClient() {
        // Set some short timeouts so this doesn't hang while testing
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setConnectTimeout(100)
                .setConnectionRequestTimeout(100)
                .setSocketTimeout(100);

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestBuilder.build())
                .build();
    }

    public static byte[] toByteArray(InputStream inputStream) {
        return Try.of(() -> IOUtils.toByteArray(inputStream)).get();
    }
}

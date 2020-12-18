package com.awslabs.general.helpers.interfaces;

import org.apache.http.client.HttpClient;

import java.io.FileNotFoundException;

public interface IoHelper {
    void writeFile(String filename, String contents) throws FileNotFoundException;

    String readFile(String filename);

    void download(String url, String outputFilename);

    boolean exists(String filename);

    HttpClient getDefaultHttpClient();
}

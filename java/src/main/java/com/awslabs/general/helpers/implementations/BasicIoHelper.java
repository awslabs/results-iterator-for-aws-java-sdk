package com.awslabs.general.helpers.implementations;

import com.awslabs.general.helpers.interfaces.IoHelper;
import io.vavr.control.Try;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BasicIoHelper implements IoHelper {
    @Inject
    public BasicIoHelper() {
    }

    @Override
    public void writeFile(String filename, String contents) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.print(contents);
        }
    }

    @Override
    public String readFile(String filename) {
        byte[] encoded = Try.of(() -> Files.readAllBytes(Paths.get(filename))).get();
        return new String(encoded, Charset.defaultCharset());
    }


    @Override
    public void download(String url, String outputFilename) {
        Try.of(() -> {
            // From: http://stackoverflow.com/a/921400
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(outputFilename);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            return null;
        })
                .get();
    }

    @Override
    public boolean exists(String filename) {
        return Paths.get(filename).toFile().exists();
    }
}

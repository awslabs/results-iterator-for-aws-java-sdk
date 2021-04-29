package com.awslabs.ecr;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.messages.ProgressMessage;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class BasicProgressHandler implements ProgressHandler {
    private final Logger log = LoggerFactory.getLogger(BasicProgressHandler.class);
    String lastMessage = null;

    @Inject
    public BasicProgressHandler() {
    }

    @Override
    public void progress(ProgressMessage message) {
        Option<String> messageOption = Option.of(message.error())
                .map(error -> String.join("", "Docker build error [", message.error(), "]"))
                .orElse(() -> Option.of(message.status()))
                .map(status -> String.join("", "Status: ", status))
                .orElse(() -> Option.of(message.progress()))
                .map(progress -> String.join("", "Progress: ", progress))
                .orElse(() -> Option.of(message.stream()))
                .map(stream -> String.join("", "Stream: ", stream));

        if (messageOption.isEmpty()) {
            return;
        }

        log.info(messageOption.get().trim());
        lastMessage = messageOption.get();
    }
}

package com.awslabs.general.helpers.interfaces;

import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface ProcessHelper {
    ProcessBuilder getProcessBuilder(List<String> programAndArguments);

    Optional<Integer> getOutputFromProcess(Logger logger, ProcessBuilder pb, boolean waitForExit, Optional<Consumer<String>> stdoutConsumer, Optional<Consumer<String>> stderrConsumer);
}

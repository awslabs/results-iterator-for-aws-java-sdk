package com.awslabs.general.helpers.interfaces;

import com.awslabs.general.helpers.data.ProcessOutput;
import io.vavr.collection.List;

import java.util.Optional;

public interface ProcessHelper {
    ProcessBuilder getProcessBuilder(List<String> programAndArguments);

    Optional<ProcessOutput> getOutputFromProcess(ProcessBuilder processBuilder);
}

package com.awslabs.general.helpers.interfaces;

import com.awslabs.general.helpers.data.ProcessOutput;
import io.vavr.collection.List;
import io.vavr.control.Option;

public interface ProcessHelper {
    ProcessBuilder getProcessBuilder(List<String> programAndArguments);

    Option<ProcessOutput> getOutputFromProcess(ProcessBuilder processBuilder);
}

package com.awslabs.general.helpers.implementations;

import com.awslabs.general.helpers.data.ImmutableProcessOutput;
import com.awslabs.general.helpers.data.ProcessOutput;
import com.awslabs.general.helpers.interfaces.ProcessHelper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BasicProcessHelper implements ProcessHelper {
    private final Logger log = LoggerFactory.getLogger(BasicProcessHelper.class);
    private static final String AWS_SECRET_KEY = "AWS_SECRET_KEY";
    private static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
    private static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
    private static final String AWS_ACCESS_KEY = "AWS_ACCESS_KEY";

    @Inject
    // Minor hack for integration tests
    public AwsCredentials awsCredentials;

    @Inject
    public BasicProcessHelper() {
    }

    @Override
    public ProcessBuilder getProcessBuilder(List<String> programAndArguments) {
        List<String> output = List.empty();

        if (SystemUtils.IS_OS_WINDOWS) {
            output = List.of("cmd.exe", "/C");
        }

        output = output.appendAll(programAndArguments);

        ProcessBuilder processBuilder = new ProcessBuilder(output.toJavaList());

        // Add in the access key ID and secret access key for when we are running processes that need them like IDT
        java.util.Map<String, String> environment = processBuilder.environment();
        // NOTE: Device Tester v1.2 does not work in Docker without AWS_ACCESS_KEY and AWS_SECRET_KEY in the environment
        environment.put(AWS_ACCESS_KEY, awsCredentials.accessKeyId());
        environment.put(AWS_ACCESS_KEY_ID, awsCredentials.accessKeyId());
        environment.put(AWS_SECRET_KEY, awsCredentials.secretAccessKey());
        environment.put(AWS_SECRET_ACCESS_KEY, awsCredentials.secretAccessKey());

        return processBuilder;
    }

    @Override
    public Option<ProcessOutput> getOutputFromProcess(ProcessBuilder processBuilder) {
        return Try.of(() -> innerGetOutputFromProcess(processBuilder))
                .recover(Exception.class, this::logExceptionMessageAndReturnEmpty)
                .get();
    }

    private Option<ProcessOutput> logExceptionMessageAndReturnEmpty(Exception throwable) {
        log.error(throwable.getMessage());

        return Option.none();
    }

    private Option<ProcessOutput> innerGetOutputFromProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        Process process = processBuilder.start();

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Wait for the process to exit
        process.waitFor();

        ProcessOutput processOutput = ImmutableProcessOutput.builder()
                .exitCode(process.exitValue())
                .standardErrorStrings(stderr.lines().collect(List.collector()))
                .standardOutStrings(stdout.lines().collect(List.collector()))
                .build();

        return Option.of(processOutput);
    }
}

package com.awslabs.general.helpers.implementations;

import com.awslabs.general.helpers.interfaces.LambdaPackagingHelper;
import com.awslabs.general.helpers.interfaces.ProcessHelper;
import com.awslabs.lambda.data.FunctionName;
import com.awslabs.lambda.data.JavaLambdaFunctionDirectory;
import com.awslabs.lambda.data.PythonLambdaFunctionDirectory;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BasicLambdaPackagingHelper implements LambdaPackagingHelper {
    public static final String PIP_3 = "pip3";
    private final Logger log = LoggerFactory.getLogger(BasicLambdaPackagingHelper.class);
    private static final String PACKAGE_DIRECTORY = "package";
    private static final String REQUIREMENTS_TXT = "requirements.txt";

    @Inject
    ProcessHelper processHelper;

    @Inject
    public BasicLambdaPackagingHelper() {
    }

    @Override
    public Path packagePythonFunction(FunctionName functionName, PythonLambdaFunctionDirectory pythonLambdaFunctionDirectory) {
        File baseDirectory = pythonLambdaFunctionDirectory.getDirectory();

        // Determine the absolute path of the package directory
        File absolutePackageDirectory = new File(String.join("/", baseDirectory.getAbsolutePath(), PACKAGE_DIRECTORY));

        // Determine what the output ZIP file name will be
        String zipFileName = String.join(".", functionName.getName(), "zip");
        Path zipFilePath = baseDirectory.toPath().resolve(zipFileName);

        // Delete any existing package directory
        cleanUpPackageDirectory(absolutePackageDirectory);

        // Delete any existing ZIP file
        Try.of(() -> Files.deleteIfExists(zipFilePath.toAbsolutePath())).get();

        // Get a snapshot of all of the files we need to copy to the package directory
        List<Path> filesToCopyToPackageDirectory = getDirectorySnapshot(baseDirectory.toPath());

        if (hasDependencies(baseDirectory.toPath())) {
            log.info(String.join("-", functionName.getName(), "Retrieving Python dependencies"));

            // Install the requirements in a package directory
            List<String> programAndArguments = new ArrayList<>();
            programAndArguments.add(PIP_3);
            programAndArguments.add("install");
            programAndArguments.add("-r");
            programAndArguments.add(REQUIREMENTS_TXT);
            programAndArguments.add("-t");
            programAndArguments.add(absolutePackageDirectory.getPath());

            ProcessBuilder processBuilder = processHelper.getProcessBuilder(programAndArguments);
            processBuilder.directory(baseDirectory);

            List<String> stdoutStrings = new ArrayList<>();
            List<String> stderrStrings = new ArrayList<>();

            Optional<Integer> exitVal = processHelper.getOutputFromProcess(log, processBuilder, true, Optional.of(stdoutStrings::add), Optional.of(stderrStrings::add));

            checkPipStatus(exitVal, stdoutStrings, stderrStrings);
        } else {
            log.info(String.join("-", functionName.getName(), "No Python dependencies to install"));
        }

        // Now the dependencies are in the directory, copy the rest of the necessary files in
        filesToCopyToPackageDirectory.forEach(file -> copyToDirectory(file, absolutePackageDirectory));

        // Package up everything into a deployment package ZIP file
        ZipUtil.pack(absolutePackageDirectory, zipFilePath.toFile());

        // Delete the package directory
        cleanUpPackageDirectory(absolutePackageDirectory);

        return zipFilePath;
    }

    @Override
    public void packageJavaFunction(JavaLambdaFunctionDirectory javaLambdaFunctionDirectory) {
        // Guidance from: https://discuss.gradle.org/t/how-to-execute-a-gradle-task-from-java-code/7421
        Try.withResources(() -> getProjectConnection(javaLambdaFunctionDirectory.getDirectory()))
                .of(this::runBuild)
                .get();
    }

    private ProjectConnection getProjectConnection(File gradleBuildPath) {
        return GradleConnector.newConnector()
                .forProjectDirectory(gradleBuildPath)
                .connect();
    }

    private Void runBuild(ProjectConnection projectConnection) {
        // Build with gradle and send the output to stdout
        BuildLauncher build = projectConnection.newBuild();
        build.forTasks("build");
        build.setStandardOutput(System.out);
        build.setStandardError(System.err);
        build.run();

        return null;
    }

    private void cleanUpPackageDirectory(File absolutePackageDirectory) {
        // Delete any existing package directory
        Try.run(() -> FileUtils.deleteDirectory(absolutePackageDirectory)).get();
    }

    private void copyToDirectory(Path path, File destination) {
        File file = path.toFile();

        if (file.isDirectory()) {
            File dirDestination = new File(String.join("/", destination.getPath(), file.getName()));
            Try.run(() -> FileUtils.copyDirectory(file, dirDestination)).get();
        } else {
            Try.run(() -> FileUtils.copyToDirectory(file, destination)).get();
        }
    }

    public boolean hasDependencies(Path buildDirectory) {
        return buildDirectory.resolve(REQUIREMENTS_TXT).toFile().exists();
    }

    private void checkPipStatus(Optional<Integer> exitVal, List<String> stdoutStrings, List<String> stderrStrings) {
        if (!exitVal.isPresent() || exitVal.get() != 0) {
            log.error("Something went wrong with pip");

            if (stderrStrings.stream().anyMatch(string -> string.contains("'clang' failed"))) {
                stdoutStrings.forEach(log::warn);
                stderrStrings.forEach(log::error);

                log.error("Building this function failed because a dependency failed to compile. This can happen when a dependency needs to build a native library. Error messages are above.");

                System.exit(1);
            }

            if (stderrStrings.stream().anyMatch(string -> string.contains("Could not find a version that satisfies the requirement")) ||
                    stderrStrings.stream().anyMatch(string -> string.contains("No matching distribution found"))) {
                stdoutStrings.forEach(log::warn);
                stderrStrings.forEach(log::error);

                log.error("Building this function failed because a dependency was not available. Error messages are above.");

                System.exit(1);
            }

            if (isCorrectPipVersion()) {
                log.error("pip version is correct but the Python dependency failed to install");
            } else {
                log.error("pip version appears to be incorrect or pip is missing");
            }

            log.error("To resolve:");
            log.error("1) Make sure Python and pip are installed and on your path");
            log.error("2) Make sure pip version is 19.x (pip --version) and install it with get-pip.py if necessary (https://pip.pypa.io/en/stable/installing/)");
            log.error("3) Try installing the dependency with pip and see if pip returns any installation errors");

            System.exit(1);
        }
    }

    private boolean isCorrectPipVersion() {
        List<String> programAndArguments = new ArrayList<>();
        programAndArguments.add(PIP_3);
        programAndArguments.add("--version");

        ProcessBuilder processBuilder = processHelper.getProcessBuilder(programAndArguments);

        String stdoutStrings = "";

        processHelper.getOutputFromProcess(log, processBuilder, true, Optional.of(stdoutStrings::concat), Optional.empty());

        // We expect pip 19.x only!
        return stdoutStrings.startsWith("pip 19.");
    }

    private List<Path> getDirectorySnapshot(Path directory) {
        return Try.of(() -> Files.list(directory).collect(Collectors.toList())).get();
    }
}
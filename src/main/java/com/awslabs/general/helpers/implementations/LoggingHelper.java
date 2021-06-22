package com.awslabs.general.helpers.implementations;

import org.jetbrains.annotations.NotNull;

public class LoggingHelper {
    public static void info() {
        System.out.println(getOutputString(null));
    }

    public static void info(String message) {
        System.out.println(getOutputString(message));
    }

    public static void error() {
        System.err.println(getOutputString(null));
    }

    public static void error(String message) {
        System.err.println(getOutputString(message));
    }

    @NotNull
    private static String getOutputString(String message) {
        // Go up two stack frames
        StackTraceElement stackTraceElement = new Exception().getStackTrace()[2];

        // Print the class name, method name, line number, and message
        String output = String.join(":", stackTraceElement.getClassName(), stackTraceElement.getMethodName(), String.valueOf(stackTraceElement.getLineNumber()));

        if (message != null) {
            output = String.join(" - ", output, message);
        }

        return output;
    }
}

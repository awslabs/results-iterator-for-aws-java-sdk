package com.awslabs.general.helpers.interfaces;

import com.awslabs.lambda.data.FunctionName;
import com.awslabs.lambda.data.JavaLambdaFunctionDirectory;
import com.awslabs.lambda.data.PythonLambdaFunctionDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

public interface LambdaPackagingHelper {
    Path packagePythonFunction(FunctionName functionName, PythonLambdaFunctionDirectory pythonLambdaFunctionDirectory);

    @NotNull
    Path getPythonZipFilePath(FunctionName functionName, PythonLambdaFunctionDirectory pythonLambdaFunctionDirectory);

    void packageJavaFunction(JavaLambdaFunctionDirectory javaLambdaFunctionDirectory);
}

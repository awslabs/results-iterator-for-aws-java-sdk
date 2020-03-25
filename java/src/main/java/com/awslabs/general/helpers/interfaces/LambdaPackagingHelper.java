package com.awslabs.general.helpers.interfaces;

import com.awslabs.lambda.data.FunctionName;
import com.awslabs.lambda.data.PythonLambdaFunctionDirectory;

import java.nio.file.Path;

public interface LambdaPackagingHelper {
    Path packagePythonFunction(FunctionName functionName, PythonLambdaFunctionDirectory pythonLambdaFunctionDirectory);
}

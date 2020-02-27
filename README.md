## Results Iterator For AWS Java SDK

A library that handles iterating over results from the AWS Java SDK with minimal boilerplate

## How do I include it in my Gradle project?

1. Add the jitpack repo to the repositories section

    ```
    maven { url 'https://jitpack.io' }
    ```

2. Add the dependency version [(replace x.y.z with the appropriate version from the JitPack site)](https://jitpack.io/#awslabs/results-iterator-for-aws-java-sdk)

    ```
    def resultsIteratorForAwsJavaSdkVersion = '0.6.4'
    ```

3. Add the dependency to the dependencies section

    ```
    compile "com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion"
    ```

## How do I use it?

Check out an [example in the IoT reference architectures repo](https://github.com/aws-samples/iot-reference-architectures/tree/master/results-iterator-jitpack/java).

## License

This library is licensed under the Apache 2.0 License. 

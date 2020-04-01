## Results Iterator For AWS Java SDK

A library that handles iterating over results from the AWS Java SDK with minimal boilerplate. It also contains additional convenience functions that make the AWS Java SDKs even easier to work with.

## How do I include it in my Gradle project?

1. Add the jitpack repo to the repositories section

    ```
    maven { url 'https://jitpack.io' }
    ```

2. Add the dependency version [(replace x.y.z with the appropriate version from the JitPack site)](https://jitpack.io/#awslabs/results-iterator-for-aws-java-sdk)

    ```
    def resultsIteratorForAwsJavaSdkVersion = 'x.y.z'
    ```

3. Add the dependency to the dependencies section

    ```
    compile "com.github.awslabs:results-iterator-for-aws-java-sdk:$resultsIteratorForAwsJavaSdkVersion"
    ```

## How do I use it?

Check out an [example in the IoT reference architectures repo](https://github.com/aws-samples/iot-reference-architectures/tree/master/results-iterator-jitpack/java).

## What are some of the other features that are useful?

### Automatic or manual configuration

If you're using Dagger you can use the V1HelperModule and V2HelperModule classes in your own components (AKA injectors)
to get SDK clients and helper objects that are constructed with sane defaults.

If you need to configure the SDK client classes you can instead get a builder that is pre-populated with the default
values that can all be overridden.

See the `TestV1ResultsIterator` and `TestV2ResultsIterator` test classes to see how the normal classes are used.

The `TestV2ResultsIterator.listAll` method shows how to use a builder provider to build an S3Client object that connects
to a different region when a bucket is not located in the region that the default injected S3Client is configured for.

## License

This library is licensed under the Apache 2.0 License. 

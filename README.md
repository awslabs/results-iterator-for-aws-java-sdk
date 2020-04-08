## Results Iterator For AWS Java SDK

A library that handles iterating over results from the AWS Java SDK with minimal boilerplate. It also contains additional convenience functions that make the AWS Java SDKs even easier to work with.

## Highlights

### IoT Credential Provider support

[AWS allows applications to use certificates registered in AWS IoT to make direct calls to AWS services](https://docs.aws.amazon.com/iot/latest/developerguide/authorizing-direct-aws.html). [Using this requires a small amount of setup](docs/CREDENTIAL_PROVIDER.md) but it is the best way to make sure hard-coded IAM credentials are not being shared.

### SDK v1 and SDK v2 support

You can use either the v1 SDK or the v2 SDK.

Note: The functions supported for each SDK are different. They are added as a result of user requests. If you need a specific function, please create a Github issue.

### Dagger support

This library has Dagger modules for the v1 and v2 SDK that simplify the setup of AWS SDK clients and the classes that enable the iteration functions.

[v1 Dagger helper module](https://github.com/awslabs/results-iterator-for-aws-java-sdk/blob/master/java/src/main/java/com/awslabs/resultsiterator/v1/V1HelperModule.java)

[v2 Dagger helper module](https://github.com/awslabs/results-iterator-for-aws-java-sdk/blob/master/java/src/main/java/com/awslabs/resultsiterator/v2/V2HelperModule.java)

### Stream support

This library is designed to return streams. There is no need to worry about pagination as those details are all taken care of for you.

Example: Find all buckets you have access to that start with "sagemaker":

``` java
  ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
  V2ResultsIterator<Bucket> bucketIterator = new V2ResultsIterator<>(s3Client, listBucketsRequest);
  List<Bucket> sageMakerBuckets = bucketIterator.stream()
                .filter(bucket -> bucket.name().startsWith("sagemaker"))
                .collect(Collectors.toList());
```

### Multi-region S3 support

If you work with S3 buckets in different regions this library will automatically give you the correct, region-specific client:

Example: Get a region-specific S3 client for a bucket, regardless of your default region:

``` java
  S3Client regionSpecificS3Client = v2S3Helper.getRegionSpecificClientForBucket(bucket);
```

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

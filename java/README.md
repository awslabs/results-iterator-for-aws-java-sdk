Results iterator for AWS SDK
============================

News
----

- Release 0.5.x
  - NOTE: This release is a breaking change from the previous series
  - No more lists! Everything returned from the library now comes back as a stream. This can help avoid eagerly fetching results that a user only needs part of or that would not fit in memory.

This example code contains a library that can be used to iterate over results from calls to the AWS SDK without duplicating code.

To see how it is used look at the tests in the [BasicResultsIteratorTest class in the Jitpack project](../../results-iterator-jitpack/java/src/test/java/com/awslabs/aws/iot/resultsiterator/BasicResultsIteratorTest.java).

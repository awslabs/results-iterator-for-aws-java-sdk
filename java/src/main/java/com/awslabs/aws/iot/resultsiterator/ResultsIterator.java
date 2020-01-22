package com.awslabs.aws.iot.resultsiterator;

import java.util.stream.Stream;

public interface ResultsIterator<T> {
    Stream<T> stream();
}

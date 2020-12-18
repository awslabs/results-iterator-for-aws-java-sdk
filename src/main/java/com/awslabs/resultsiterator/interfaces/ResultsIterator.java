package com.awslabs.resultsiterator.interfaces;

import java.util.stream.Stream;

public interface ResultsIterator<T> {
    Stream<T> stream();
}

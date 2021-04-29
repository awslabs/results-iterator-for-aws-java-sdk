package com.awslabs.resultsiterator.interfaces;

import io.vavr.collection.Stream;

public interface ResultsIteratorInterface<T> {
    Stream<T> stream();
}

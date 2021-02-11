package com.awslabs.resultsiterator.interfaces;

import io.vavr.collection.Stream;

public interface ResultsIterator<T> {
    Stream<T> stream();
}

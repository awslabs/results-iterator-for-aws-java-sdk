package com.awslabs;

import com.awslabs.general.helpers.implementations.BasicJsonHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class TestHelper {
    private static final Logger log = LoggerFactory.getLogger(TestHelper.class);

    public static String testNotMeaningfulWithoutError(String nameOfRequiredObjects) {
        return String.join(" ", "This test is not meaningful unless one or more", nameOfRequiredObjects, "are defined");
    }

    public static String testNotMeaningfulWithoutAtLeastError(String nameOfRequiredObjects, long count) {
        return String.join(" ", "This test is not meaningful unless " + count + " or more", nameOfRequiredObjects, "are defined");
    }

    public static boolean streamNotEmpty(Stream stream) {
        return stream.findFirst().isPresent();
    }

    public static <T> void testNotMeaningfulWithout(String nameOfRequiredObjects, Stream<T> stream) {
        assertTrue(testNotMeaningfulWithoutError(nameOfRequiredObjects), streamNotEmpty(stream));
    }

    public static <T> void testNotMeaningfulWithoutAtLeast(String nameOfRequiredObjects, Stream<T> stream, long count) {
        assertTrue(testNotMeaningfulWithoutAtLeastError(nameOfRequiredObjects, count), stream.count() >= count);
    }

    public static <T> void testNotMeaningfulWithout(String nameOfRequiredObjects, long itemCount) {
        assertTrue(testNotMeaningfulWithoutError(nameOfRequiredObjects), itemCount > 0);
    }

    public static <T> long logAndCount(Optional<List<T>> optionalObjects) {
        if (!optionalObjects.isPresent()) {
            return 0;
        }

        return logObjects(optionalObjects.get());
    }

    private static <T> long logObjects(List<T> objects) {
        JsonHelper jsonHelper = new BasicJsonHelper();

        log.info(jsonHelper.toJson(objects));

        return objects.size();
    }

    private static <T> long logObject(T object) {
        JsonHelper jsonHelper = new BasicJsonHelper();

        log.info(jsonHelper.toJson(object));

        return 1;
    }

    public static <T> long logAndCount(Stream<T> stream) {
        return stream.map(TestHelper::logObject)
                .reduce(0L, Long::sum);
    }
}

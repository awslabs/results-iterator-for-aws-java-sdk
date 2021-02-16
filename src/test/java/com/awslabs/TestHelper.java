package com.awslabs;

import com.awslabs.general.helpers.implementations.BasicJsonHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class TestHelper {
    private static final Logger log = LoggerFactory.getLogger(TestHelper.class);

    public static String testNotMeaningfulWithoutError(String nameOfRequiredObjects) {
        return String.join(" ", "This test is not meaningful without one (or more, if applicable)", nameOfRequiredObjects);
    }

    public static String testNotMeaningfulWithoutAtLeastError(String nameOfRequiredObjects, long count) {
        return String.join(" ", "This test is not meaningful unless", String.valueOf(count), "or more", nameOfRequiredObjects, "are defined");
    }

    public static <T> void testNotMeaningfulWithout(String nameOfRequiredObjects, Stream<T> stream) {
        assertTrue(testNotMeaningfulWithoutError(nameOfRequiredObjects), stream.nonEmpty());
    }

    public static <T> void testNotMeaningfulWithoutAtLeast(String nameOfRequiredObjects, Stream<T> stream, long count) {
        assertTrue(testNotMeaningfulWithoutAtLeastError(nameOfRequiredObjects, count), stream.size() >= count);
    }

    public static void testNotMeaningfulWithout(String nameOfRequiredObjects, int itemCount) {
        assertTrue(testNotMeaningfulWithoutError(nameOfRequiredObjects), itemCount > 0);
    }

    public static <T> int logAndCount(Option<List<T>> objectsOption) {
        if (objectsOption.isEmpty()) {
            return 0;
        }

        return logObjects(objectsOption.get());
    }

    private static <T> int logObjects(List<T> objects) {
        JsonHelper jsonHelper = new BasicJsonHelper();

        log.info(jsonHelper.toJson(objects));

        return objects.size();
    }

    public static <T> int logObject(T object) {
        JsonHelper jsonHelper = new BasicJsonHelper();

        log.info(jsonHelper.toJson(object));

        return 1;
    }

    public static <T> int logAndCount(Stream<T> stream) {
        return stream
                .map(TestHelper::logObject)
                .size();
    }
}

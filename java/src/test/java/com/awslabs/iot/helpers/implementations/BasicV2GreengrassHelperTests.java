package com.awslabs.iot.helpers.implementations;

import com.awslabs.TestHelper;
import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.helpers.interfaces.V2GreengrassHelper;
import com.awslabs.resultsiterator.v2.implementations.DaggerV2TestInjector;
import com.awslabs.resultsiterator.v2.implementations.V2TestInjector;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.greengrass.GreengrassClient;
import software.amazon.awssdk.services.greengrass.model.GroupInformation;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.awslabs.TestHelper.testNotMeaningfulWithout;

public class BasicV2GreengrassHelperTests {
    private final Logger log = LoggerFactory.getLogger(BasicV2GreengrassHelperTests.class);
    private GreengrassClient greengrassClient;
    private JsonHelper jsonHelper;
    private V2GreengrassHelper v2GreengrassHelper;

    @Before
    public void setup() {
        V2TestInjector injector = DaggerV2TestInjector.create();
        v2GreengrassHelper = injector.v2GreengrassHelper();
        greengrassClient = injector.greengrassClient();
        jsonHelper = injector.jsonHelper();
    }

    @Test
    public void shouldListGreengrassGroupsWithHelperAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        List<GroupInformation> groupInformationList = getStream.call().collect(Collectors.toList());
        groupInformationList.forEach(groupInformation -> log.info(jsonHelper.toJson(groupInformation)));
    }

    @Test
    public void shouldListGreengrassGroupSubscriptionsAndNotThrowAnException() throws Exception {
        Callable<Stream<GroupInformation>> getStream = () -> v2GreengrassHelper.getGroups();
        testNotMeaningfulWithout("Greengrass groups", getStream.call());

        List<GroupInformation> groupInformationList = getStream.call().collect(Collectors.toList());

        Long numberOfSubscriptions = groupInformationList.stream()
                .map(groupInformation -> v2GreengrassHelper.getSubscriptions(groupInformation))
                .map(TestHelper::logAndCount)
                .reduce(0L, Long::sum);

        testNotMeaningfulWithout("subscriptions", numberOfSubscriptions);
    }
}

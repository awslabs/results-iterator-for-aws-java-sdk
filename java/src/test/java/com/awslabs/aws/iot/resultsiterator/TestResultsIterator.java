package com.awslabs.aws.iot.resultsiterator;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ThingAttribute;
import org.junit.Test;

import java.util.List;

public class TestResultsIterator {
    @Test
    public void shouldListThingAttributesAndNotThrowAnException() {
        AWSIotClient awsIotClient = (AWSIotClient) AWSIotClient.builder().build();
        ListThingsRequest listThingsRequest = new ListThingsRequest();
        ResultsIterator<ThingAttribute> resultsIterator = new ResultsIterator<>(awsIotClient, listThingsRequest);
        List<ThingAttribute> thingAttributes = resultsIterator.iterateOverResults();
        thingAttributes.forEach(System.out::println);
    }
}

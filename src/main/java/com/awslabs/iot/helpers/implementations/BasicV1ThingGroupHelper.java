package com.awslabs.iot.helpers.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.DeleteThingGroupRequest;
import com.amazonaws.services.iot.model.GroupNameAndArn;
import com.amazonaws.services.iot.model.ListThingGroupsRequest;
import com.awslabs.iot.helpers.interfaces.V1ThingGroupHelper;
import com.awslabs.resultsiterator.v1.implementations.V1ResultsIterator;
import io.vavr.collection.Stream;
import org.slf4j.Logger;

import javax.inject.Inject;

public class BasicV1ThingGroupHelper implements V1ThingGroupHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1ThingGroupHelper.class);
    @Inject
    AWSIotClient awsIotClient;

    @Inject
    public BasicV1ThingGroupHelper() {
    }

    @Override
    public Stream<GroupNameAndArn> listThingGroups() {
        return new V1ResultsIterator<GroupNameAndArn>(awsIotClient, ListThingGroupsRequest.class).stream();
    }

    @Override
    public void deleteThingGroup(String thingGroupName) {
        awsIotClient.deleteThingGroup(new DeleteThingGroupRequest().withThingGroupName(thingGroupName));
    }
}

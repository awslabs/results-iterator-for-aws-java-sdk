package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.DeleteThingGroupRequest;
import com.amazonaws.services.iot.model.GroupNameAndArn;
import com.amazonaws.services.iot.model.ListThingGroupsRequest;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1ThingGroupHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

public class BasicV1ThingGroupHelper implements V1ThingGroupHelper {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BasicV1ThingGroupHelper.class);
    @Inject
    AWSIotClient awsIotClient;

    @Inject
    public BasicV1ThingGroupHelper() {
    }

    @Override
    public List<GroupNameAndArn> listThingGroups() {
        List<GroupNameAndArn> groupNamesAndArns = new V1ResultsIterator<GroupNameAndArn>(awsIotClient, ListThingGroupsRequest.class).iterateOverResults();

        return groupNamesAndArns;
    }

    @Override
    public void deleteThingGroup(String thingGroupName) {
        awsIotClient.deleteThingGroup(new DeleteThingGroupRequest().withThingGroupName(thingGroupName));
    }
}

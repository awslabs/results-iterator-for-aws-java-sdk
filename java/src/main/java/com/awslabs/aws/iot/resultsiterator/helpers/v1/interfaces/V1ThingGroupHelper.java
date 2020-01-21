package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.GroupNameAndArn;

import java.util.List;

public interface V1ThingGroupHelper {
    List<GroupNameAndArn> listThingGroups();

    void deleteThingGroup(String thingGroupName);
}

package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.iot.model.GroupNameAndArn;

import java.util.stream.Stream;

public interface V1ThingGroupHelper {
    Stream<GroupNameAndArn> listThingGroups();

    void deleteThingGroup(String thingGroupName);
}

package com.awslabs.iot.helpers.interfaces;

import com.amazonaws.services.iot.model.GroupNameAndArn;

import java.util.stream.Stream;

public interface V1ThingGroupHelper {
    Stream<GroupNameAndArn> listThingGroups();

    void deleteThingGroup(String thingGroupName);
}

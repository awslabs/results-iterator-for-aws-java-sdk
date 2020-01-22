package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.identitymanagement.model.Role;

import java.util.stream.Stream;

public interface V1IamHelper {
    Stream<Role> listRoles();

    Stream<String> listRoleNames();

    String getRoleArn(String roleName);
}

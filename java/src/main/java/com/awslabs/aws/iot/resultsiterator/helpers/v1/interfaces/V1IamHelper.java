package com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces;

import com.amazonaws.services.identitymanagement.model.Role;

import java.util.List;

public interface V1IamHelper {
    List<Role> listRoles();

    List<String> listRoleNames();

    String getRoleArn(String roleName);
}

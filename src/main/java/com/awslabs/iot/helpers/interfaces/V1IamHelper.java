package com.awslabs.iot.helpers.interfaces;

import com.amazonaws.services.identitymanagement.model.Role;
import io.vavr.collection.Stream;

public interface V1IamHelper {
    Stream<Role> listRoles();

    Stream<String> listRoleNames();

    String getRoleArn(String roleName);
}

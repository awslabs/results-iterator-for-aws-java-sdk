package com.awslabs.iot.helpers.implementations;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.awslabs.iot.helpers.interfaces.V1IamHelper;
import com.awslabs.resultsiterator.v1.implementations.V1ResultsIterator;
import io.vavr.collection.Stream;

import javax.inject.Inject;

public class BasicV1IamHelper implements V1IamHelper {
    @Inject
    AmazonIdentityManagementClient amazonIdentityManagementClient;

    @Inject
    public BasicV1IamHelper() {
    }

    @Override
    public Stream<Role> listRoles() {
        return new V1ResultsIterator<Role>(amazonIdentityManagementClient, ListRolesRequest.class).stream();
    }

    @Override
    public Stream<String> listRoleNames() {
        return listRoles().map(Role::getRoleName);
    }

    @Override
    public String getRoleArn(String roleName) {
        GetRoleRequest getRoleRequest = new GetRoleRequest()
                .withRoleName(roleName);

        GetRoleResult getRoleResult = amazonIdentityManagementClient.getRole(getRoleRequest);

        Role role = getRoleResult.getRole();

        if (role == null) {
            return null;
        }

        return role.getArn();
    }
}

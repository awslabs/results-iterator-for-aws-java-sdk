package com.awslabs.aws.iot.resultsiterator.helpers.v1.implementations;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.V1ResultsIterator;
import com.awslabs.aws.iot.resultsiterator.helpers.v1.interfaces.V1IamHelper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class BasicV1IamHelper implements V1IamHelper {
    @Inject
    AmazonIdentityManagementClient amazonIdentityManagementClient;

    @Inject
    public BasicV1IamHelper() {
    }

    @Override
    public List<Role> listRoles() {
        List<Role> roles = new V1ResultsIterator<Role>(amazonIdentityManagementClient, ListRolesRequest.class).iterateOverResults();

        return roles;
    }

    @Override
    public List<String> listRoleNames() {
        List<Role> roles = listRoles();

        List<String> roleNames = new ArrayList<>();

        for (Role role : roles) {
            roleNames.add(role.getRoleName());
        }

        return roleNames;
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

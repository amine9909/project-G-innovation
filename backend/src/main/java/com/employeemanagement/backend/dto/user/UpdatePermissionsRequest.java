package com.employeemanagement.backend.dto.user;

import com.employeemanagement.backend.enums.Permission;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class UpdatePermissionsRequest {

    @NotNull
    private Set<Permission> permissions;

    public UpdatePermissionsRequest() {
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}

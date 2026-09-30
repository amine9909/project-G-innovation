package com.employeemanagement.backend.dto.user;

import com.employeemanagement.backend.enums.Permission;
import com.employeemanagement.backend.enums.UserType;

import java.util.Set;

public class UserResponse {

    private final Long id;
    private final String name;
    private final String username;
    private final String email;
    private final UserType userType;
    private final Set<Permission> permissions;
    private final boolean active;

    public UserResponse(Long id, String name, String username, String email, UserType userType,
                        Set<Permission> permissions, boolean active) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.permissions = permissions;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserType getUserType() {
        return userType;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean isActive() {
        return active;
    }
}

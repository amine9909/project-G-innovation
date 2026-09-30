package com.employeemanagement.backend.dto.auth;

import com.employeemanagement.backend.enums.Permission;
import com.employeemanagement.backend.enums.UserType;

import java.util.Set;

public class LoginResponse {

    private final Long userId;
    private final String name;
    private final String username;
    private final String email;
    private final UserType userType;
    private final Set<Permission> permissions;

    public LoginResponse(Long userId, String name, String username, String email,
                         UserType userType, Set<Permission> permissions) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.permissions = permissions;
    }

    public Long getUserId() {
        return userId;
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
}

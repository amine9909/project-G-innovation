package com.employeemanagement.backend.dto.auth;

public class PasswordResetMessageResponse {

    private final String message;

    public PasswordResetMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

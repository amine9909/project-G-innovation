package com.employeemanagement.backend.dto.notification;

public class UnreadNotificationCountResponse {

    private final long count;

    public UnreadNotificationCountResponse(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }
}

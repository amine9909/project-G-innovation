package com.employeemanagement.backend.dto.notification;

import java.time.Instant;

public class NotificationResponse {

    private final Long id;
    private final String actorName;
    private final String actorUsername;
    private final String action;
    private final Long targetEmployeeId;
    private final String targetEmployeeName;
    private final String targetEmployeeEmail;
    private final Instant createdAt;
    private final boolean read;

    public NotificationResponse(Long id, String actorName, String actorUsername, String action, Long targetEmployeeId,
                                String targetEmployeeName, String targetEmployeeEmail, Instant createdAt,
                                boolean read) {
        this.id = id;
        this.actorName = actorName;
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetEmployeeId = targetEmployeeId;
        this.targetEmployeeName = targetEmployeeName;
        this.targetEmployeeEmail = targetEmployeeEmail;
        this.createdAt = createdAt;
        this.read = read;
    }

    public Long getId() {
        return id;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getAction() {
        return action;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public String getTargetEmployeeName() {
        return targetEmployeeName;
    }

    public String getTargetEmployeeEmail() {
        return targetEmployeeEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}

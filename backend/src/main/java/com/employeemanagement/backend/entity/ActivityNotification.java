package com.employeemanagement.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "activity_notifications")
public class ActivityNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_employee_id")
    private Long targetEmployeeId;

    @Column(name = "target_employee_name", nullable = false)
    private String targetEmployeeName;

    @Column(name = "target_employee_email")
    private String targetEmployeeEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus = false;

    public ActivityNotification() {
    }

    public ActivityNotification(Long actorUserId, String actorName, String actorUsername, String action,
                                Long targetEmployeeId, String targetEmployeeName, String targetEmployeeEmail,
                                Instant createdAt, boolean readStatus) {
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetEmployeeId = targetEmployeeId;
        this.targetEmployeeName = targetEmployeeName;
        this.targetEmployeeEmail = targetEmployeeEmail;
        this.createdAt = createdAt;
        this.readStatus = readStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(Long targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
    }

    public String getTargetEmployeeName() {
        return targetEmployeeName;
    }

    public void setTargetEmployeeName(String targetEmployeeName) {
        this.targetEmployeeName = targetEmployeeName;
    }

    public String getTargetEmployeeEmail() {
        return targetEmployeeEmail;
    }

    public void setTargetEmployeeEmail(String targetEmployeeEmail) {
        this.targetEmployeeEmail = targetEmployeeEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }
}

package com.employeemanagement.backend.controller;

import com.employeemanagement.backend.dto.notification.NotificationResponse;
import com.employeemanagement.backend.dto.notification.UnreadNotificationCountResponse;
import com.employeemanagement.backend.service.ActivityNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class NotificationController {

    private final ActivityNotificationService activityNotificationService;

    public NotificationController(ActivityNotificationService activityNotificationService) {
        this.activityNotificationService = activityNotificationService;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<NotificationResponse>> getLatestNotifications() {
        return ResponseEntity.ok(activityNotificationService.getLatestNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount() {
        return ResponseEntity.ok(new UnreadNotificationCountResponse(activityNotificationService.getUnreadCount()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(activityNotificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        activityNotificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}

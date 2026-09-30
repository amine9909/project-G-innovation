package com.employeemanagement.backend.service;

import com.employeemanagement.backend.dto.notification.NotificationResponse;
import com.employeemanagement.backend.entity.ActivityNotification;
import com.employeemanagement.backend.entity.Employee;
import com.employeemanagement.backend.entity.User;
import com.employeemanagement.backend.repository.ActivityNotificationRepository;
import com.employeemanagement.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityNotificationService {

    public static final String EMPLOYEE_CREATED = "EMPLOYEE_CREATED";
    public static final String EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
    public static final String EMPLOYEE_DELETED = "EMPLOYEE_DELETED";
    public static final String PHOTO_UPLOADED = "PHOTO_UPLOADED";
    public static final String PHOTO_REPLACED = "PHOTO_REPLACED";
    public static final String PHOTO_DELETED = "PHOTO_DELETED";
    public static final String CV_UPLOADED = "CV_UPLOADED";
    public static final String CV_REPLACED = "CV_REPLACED";
    public static final String CV_DELETED = "CV_DELETED";
    public static final String EMPLOYEE_IMPORT = "EMPLOYEE_IMPORT";

    private static final String SYSTEM_ACTOR = "System";

    private final ActivityNotificationRepository activityNotificationRepository;
    private final UserRepository userRepository;

    public ActivityNotificationService(ActivityNotificationRepository activityNotificationRepository,
                                       UserRepository userRepository) {
        this.activityNotificationRepository = activityNotificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordEmployeeActivity(String action, Employee employee) {
        Actor actor = resolveActor();
        activityNotificationRepository.save(new ActivityNotification(
                actor.userId(),
                actor.name(),
                actor.username(),
                action,
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getEmail(),
                Instant.now(),
                false
        ));
    }

    @Transactional
    public void recordImportActivity(int importedCount) {
        Actor actor = resolveActor();
        activityNotificationRepository.save(new ActivityNotification(
                actor.userId(),
                actor.name(),
                actor.username(),
                EMPLOYEE_IMPORT,
                null,
                importedCount + " employees imported",
                null,
                Instant.now(),
                false
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getLatestNotifications() {
        return activityNotificationRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return activityNotificationRepository.countByReadStatusFalse();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        ActivityNotification notification = activityNotificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));

        notification.setReadStatus(true);
        return toResponse(activityNotificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead() {
        List<ActivityNotification> unreadNotifications = activityNotificationRepository.findByReadStatusFalse();
        unreadNotifications.forEach(notification -> notification.setReadStatus(true));
        activityNotificationRepository.saveAll(unreadNotifications);
    }

    private Actor resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();

        if (username == null || username.isBlank()) {
            return new Actor(null, SYSTEM_ACTOR, SYSTEM_ACTOR);
        }

        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return new Actor(null, username, username);
        }

        User actor = user.get();
        return new Actor(actor.getId(), actor.getName(), actor.getUsername());
    }

    private NotificationResponse toResponse(ActivityNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getActorName(),
                notification.getActorUsername(),
                notification.getAction(),
                notification.getTargetEmployeeId(),
                notification.getTargetEmployeeName(),
                notification.getTargetEmployeeEmail(),
                notification.getCreatedAt(),
                notification.isReadStatus()
        );
    }

    private record Actor(Long userId, String name, String username) {
    }
}

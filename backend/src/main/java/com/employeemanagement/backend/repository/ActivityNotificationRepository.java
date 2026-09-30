package com.employeemanagement.backend.repository;

import com.employeemanagement.backend.entity.ActivityNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityNotificationRepository extends JpaRepository<ActivityNotification, Long> {

    List<ActivityNotification> findTop10ByOrderByCreatedAtDesc();

    long countByReadStatusFalse();

    List<ActivityNotification> findByReadStatusFalse();
}

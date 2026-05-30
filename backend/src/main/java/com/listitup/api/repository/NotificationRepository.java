package com.listitup.api.repository;

import com.listitup.api.model.Notification;
import com.listitup.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);
    long countByUserAndIsReadFalse(User user);
}

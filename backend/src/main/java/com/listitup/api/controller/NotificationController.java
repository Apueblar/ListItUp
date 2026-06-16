package com.listitup.api.controller;

import com.listitup.api.model.Notification;
import com.listitup.api.model.User;
import com.listitup.api.repository.NotificationRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(401).build();
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        
        List<Notification> notifications = notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        
        var dtoList = notifications.stream().map(n -> Map.of(
                "id", n.getNotificationId(),
                "message", n.getMessage(),
                "linkUrl", n.getLinkUrl() != null ? n.getLinkUrl() : "",
                "isRead", n.getIsRead(),
                "createdAt", n.getCreatedAt()
        )).collect(Collectors.toList());

        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("notifications", dtoList);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markAsRead(@AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(401).build();
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        
        // Mark all unread notifications for this user as read
        List<Notification> allNotifications = notificationRepository.findAll();
        for (Notification n : allNotifications) {
            if (n.getUser().getUserId().equals(user.getUserId()) && !n.getIsRead()) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        }
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<?> markSingleAsRead(@PathVariable java.util.UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(401).build();
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getUserId().equals(user.getUserId()) && !n.getIsRead()) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
        
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/clear-all")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> clearAll(@AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(401).build();
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        notificationRepository.deleteAllByUser(user);
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}

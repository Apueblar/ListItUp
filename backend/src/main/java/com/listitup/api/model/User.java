package com.listitup.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider;

    @Column(nullable = false)
    private String role = "STANDARD";

    @Column(name = "has_badge")
    private Boolean hasBadge = false;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "can_pin_lists")
    private Boolean canPinLists = false;

    @Column(name = "has_analytics_access")
    private Boolean hasAnalyticsAccess = false;

    @Column(name = "can_moderate_content")
    private Boolean canModerateContent = false;

    @Column(name = "can_delete_any")
    private Boolean canDeleteAny = false;

    @Column(name = "has_completed_setup")
    private Boolean hasCompletedSetup = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getHasBadge() { return hasBadge; }
    public void setHasBadge(Boolean hasBadge) { this.hasBadge = hasBadge; }
    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }
    public Boolean getCanPinLists() { return canPinLists; }
    public void setCanPinLists(Boolean canPinLists) { this.canPinLists = canPinLists; }
    public Boolean getHasAnalyticsAccess() { return hasAnalyticsAccess; }
    public void setHasAnalyticsAccess(Boolean hasAnalyticsAccess) { this.hasAnalyticsAccess = hasAnalyticsAccess; }
    public Boolean getCanModerateContent() { return canModerateContent; }
    public void setCanModerateContent(Boolean canModerateContent) { this.canModerateContent = canModerateContent; }
    public Boolean getCanDeleteAny() { return canDeleteAny; }
    public void setCanDeleteAny(Boolean canDeleteAny) { this.canDeleteAny = canDeleteAny; }
    public Boolean getHasCompletedSetup() { return hasCompletedSetup; }
    public void setHasCompletedSetup(Boolean hasCompletedSetup) { this.hasCompletedSetup = hasCompletedSetup; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package com.listitup.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id", updatable = false, nullable = false)
    private UUID reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_list_id")
    private CuratedList targetList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_item_id")
    private Item targetItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_comment_id")
    private Comment targetComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id")
    private User submittedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    @Column(name = "reason", nullable = false)
    private String reason;
    
    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, REVIEWED, RESOLVED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void validateTargets() {
        int targets = 0;
        if (targetList != null) targets++;
        if (targetItem != null) targets++;
        if (targetComment != null) targets++;
        if (targets != 1) {
            throw new IllegalStateException("Exactly one target (list, item, or comment) must be provided for a report.");
        }
    }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }
    
    public CuratedList getTargetList() { return targetList; }
    public void setTargetList(CuratedList targetList) { this.targetList = targetList; }
    
    public Item getTargetItem() { return targetItem; }
    public void setTargetItem(Item targetItem) { this.targetItem = targetItem; }
    
    public Comment getTargetComment() { return targetComment; }
    public void setTargetComment(Comment targetComment) { this.targetComment = targetComment; }
    
    public User getSubmittedByUser() { return submittedByUser; }
    public void setSubmittedByUser(User submittedByUser) { this.submittedByUser = submittedByUser; }
    
    public User getReviewedByAdmin() { return reviewedByAdmin; }
    public void setReviewedByAdmin(User reviewedByAdmin) { this.reviewedByAdmin = reviewedByAdmin; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

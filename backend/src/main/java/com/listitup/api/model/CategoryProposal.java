package com.listitup.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "category_proposals")
public class CategoryProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "proposal_id", updatable = false, nullable = false)
    private UUID proposalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User proposer;

    @Column(name = "proposed_name", nullable = false)
    private String proposedName;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getProposalId() { return proposalId; }
    public void setProposalId(UUID proposalId) { this.proposalId = proposalId; }
    public User getProposer() { return proposer; }
    public void setProposer(User proposer) { this.proposer = proposer; }
    public String getProposedName() { return proposedName; }
    public void setProposedName(String proposedName) { this.proposedName = proposedName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

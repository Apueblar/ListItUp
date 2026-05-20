package com.listitup.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_lists")
public class SavedList {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "saved_id", updatable = false, nullable = false)
    private UUID savedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private CuratedList list;

    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt = LocalDateTime.now();

    public UUID getSavedId() { return savedId; }
    public void setSavedId(UUID savedId) { this.savedId = savedId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public CuratedList getList() { return list; }
    public void setList(CuratedList list) { this.list = list; }
}

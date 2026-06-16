package com.listitup.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "list_analytics")
public class ListAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "analytics_id", updatable = false, nullable = false)
    private UUID analyticsId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false, unique = true)
    private CuratedList list;

    @Column(name = "views", nullable = false)
    private long views = 0;

    @Column(name = "saves", nullable = false)
    private long saves = 0;

    @Column(name = "link_clicks", nullable = false)
    private long linkClicks = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    // Getters and Setters
    public UUID getAnalyticsId() { return analyticsId; }
    public void setAnalyticsId(UUID analyticsId) { this.analyticsId = analyticsId; }
    public CuratedList getList() { return list; }
    public void setList(CuratedList list) { this.list = list; }
    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }
    public long getSaves() { return saves; }
    public void setSaves(long saves) { this.saves = saves; }
    public long getLinkClicks() { return linkClicks; }
    public void setLinkClicks(long linkClicks) { this.linkClicks = linkClicks; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}

package com.listitup.api.repository;

import com.listitup.api.model.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {
    List<AnalyticsSnapshot> findAllByOrderBySnapshotDateAsc();
}

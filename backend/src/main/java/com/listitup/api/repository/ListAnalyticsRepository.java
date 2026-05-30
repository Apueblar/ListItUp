package com.listitup.api.repository;

import com.listitup.api.model.ListAnalytics;
import com.listitup.api.model.CuratedList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import com.listitup.api.model.User;
import java.util.List;

public interface ListAnalyticsRepository extends JpaRepository<ListAnalytics, UUID> {
    Optional<ListAnalytics> findByList(CuratedList list);
    List<ListAnalytics> findByList_Creator(User creator);
}

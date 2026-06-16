package com.listitup.api.service;

import com.listitup.api.model.AnalyticsSnapshot;
import com.listitup.api.repository.AnalyticsSnapshotRepository;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AnalyticsService {

    private final AnalyticsSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final CuratedListRepository listRepository;
    private final EntityManager entityManager;

    public AnalyticsService(AnalyticsSnapshotRepository snapshotRepository, UserRepository userRepository, CuratedListRepository listRepository, EntityManager entityManager) {
        this.snapshotRepository = snapshotRepository;
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.entityManager = entityManager;
    }

    // Runs every day at 3 AM
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void generateDailySnapshot() {
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
        snapshot.setSnapshotDate(LocalDate.now());

        long totalUsers = userRepository.count();
        long totalLists = listRepository.count();
        
        Long totalViews = (Long) entityManager.createQuery("SELECT SUM(l.viewCount) FROM CuratedList l").getSingleResult();
        Long totalLikes = (Long) entityManager.createQuery("SELECT COUNT(l) FROM Like l").getSingleResult();
        Long totalSaves = (Long) entityManager.createQuery("SELECT COUNT(sl) FROM SavedList sl").getSingleResult();

        snapshot.setTotalUsers(totalUsers);
        snapshot.setTotalLists(totalLists);
        snapshot.setTotalViews(totalViews != null ? totalViews : 0L);
        snapshot.setTotalLikes(totalLikes != null ? totalLikes : 0L);
        snapshot.setTotalSaves(totalSaves != null ? totalSaves : 0L);

        snapshotRepository.save(snapshot);
    }
}

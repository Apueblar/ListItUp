package com.listitup.api.service;

import com.listitup.api.model.AnalyticsSnapshot;
import com.listitup.api.repository.AnalyticsSnapshotRepository;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private AnalyticsSnapshotRepository snapshotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CuratedListRepository listRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGenerateDailySnapshot_Success() {
        when(userRepository.count()).thenReturn(10L);
        when(listRepository.count()).thenReturn(5L);

        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(100L).thenReturn(20L).thenReturn(15L);

        analyticsService.generateDailySnapshot();

        ArgumentCaptor<AnalyticsSnapshot> captor = ArgumentCaptor.forClass(AnalyticsSnapshot.class);
        verify(snapshotRepository, times(1)).save(captor.capture());

        AnalyticsSnapshot saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(LocalDate.now(), saved.getSnapshotDate());
        assertEquals(10L, saved.getTotalUsers());
        assertEquals(5L, saved.getTotalLists());
        assertEquals(100L, saved.getTotalViews());
        assertEquals(20L, saved.getTotalLikes());
        assertEquals(15L, saved.getTotalSaves());
    }

    @Test
    void testGenerateDailySnapshot_NullAggregations() {
        when(userRepository.count()).thenReturn(0L);
        when(listRepository.count()).thenReturn(0L);

        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(null);

        analyticsService.generateDailySnapshot();

        ArgumentCaptor<AnalyticsSnapshot> captor = ArgumentCaptor.forClass(AnalyticsSnapshot.class);
        verify(snapshotRepository, times(1)).save(captor.capture());

        AnalyticsSnapshot saved = captor.getValue();
        assertEquals(0L, saved.getTotalViews());
        assertEquals(0L, saved.getTotalLikes());
        assertEquals(0L, saved.getTotalSaves());
    }

    @Test
    void testGenerateDailySnapshot_CheckValues() {
        when(userRepository.count()).thenReturn(250L);
        when(listRepository.count()).thenReturn(180L);

        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(5000L).thenReturn(320L).thenReturn(120L);

        analyticsService.generateDailySnapshot();

        ArgumentCaptor<AnalyticsSnapshot> captor = ArgumentCaptor.forClass(AnalyticsSnapshot.class);
        verify(snapshotRepository).save(captor.capture());

        AnalyticsSnapshot saved = captor.getValue();
        assertEquals(250L, saved.getTotalUsers());
        assertEquals(180L, saved.getTotalLists());
        assertEquals(5000L, saved.getTotalViews());
        assertEquals(320L, saved.getTotalLikes());
        assertEquals(120L, saved.getTotalSaves());
    }

    @Test
    void testSnapshotCorrectness_Properties() {
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
        UUID snapshotId = UUID.randomUUID();
        snapshot.setSnapshotId(snapshotId);
        snapshot.setSnapshotDate(LocalDate.of(2026, 6, 16));
        snapshot.setTotalUsers(50L);
        snapshot.setTotalLists(20L);
        snapshot.setTotalViews(1000L);
        snapshot.setTotalLikes(100L);
        snapshot.setTotalSaves(30L);

        assertEquals(snapshotId, snapshot.getSnapshotId());
        assertEquals(LocalDate.of(2026, 6, 16), snapshot.getSnapshotDate());
        assertEquals(50L, snapshot.getTotalUsers());
        assertEquals(20L, snapshot.getTotalLists());
        assertEquals(1000L, snapshot.getTotalViews());
        assertEquals(100L, snapshot.getTotalLikes());
        assertEquals(30L, snapshot.getTotalSaves());
    }

    @Test
    void testGenerateDailySnapshot_SaveCalled() {
        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);

        analyticsService.generateDailySnapshot();
        verify(snapshotRepository, times(1)).save(any(AnalyticsSnapshot.class));
    }

    @Test
    void testGenerateDailySnapshot_Dates() {
        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);

        analyticsService.generateDailySnapshot();

        ArgumentCaptor<AnalyticsSnapshot> captor = ArgumentCaptor.forClass(AnalyticsSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue().getSnapshotDate());
    }
}

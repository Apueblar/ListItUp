package com.listitup.api.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TrendingService {

    private static class ViewEvent {
        private final UUID listId;
        private final LocalDateTime viewedAt;

        public ViewEvent(UUID listId, LocalDateTime viewedAt) {
            this.listId = listId;
            this.viewedAt = viewedAt;
        }

        public UUID getListId() {
            return listId;
        }

        public LocalDateTime getViewedAt() {
            return viewedAt;
        }
    }

    // A thread-safe queue to store recent view events
    private final Queue<ViewEvent> viewEvents = new ConcurrentLinkedQueue<>();

    /**
     * Records a list view event at the current time.
     */
    public void recordView(UUID listId) {
        viewEvents.add(new ViewEvent(listId, LocalDateTime.now()));
        cleanUpOldViews();
    }

    /**
     * Evicts events older than 24 hours from the queue.
     */
    private void cleanUpOldViews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        while (!viewEvents.isEmpty() && viewEvents.peek().getViewedAt().isBefore(cutoff)) {
            viewEvents.poll();
        }
    }

    /**
     * Returns a map of list UUIDs to their view counts within the last 24 hours.
     */
    public Map<UUID, Integer> getRecentViewCounts() {
        cleanUpOldViews();
        Map<UUID, Integer> counts = new HashMap<>();
        for (ViewEvent event : viewEvents) {
            counts.put(event.getListId(), counts.getOrDefault(event.getListId(), 0) + 1);
        }
        return counts;
    }
}

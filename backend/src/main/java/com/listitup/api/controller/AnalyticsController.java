package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
@RequestMapping("/analytics")
@PreAuthorize("hasRole('VERIFIED')")
public class AnalyticsController {

    private final UserRepository userRepository;
    private final CuratedListRepository listRepository;
    private final EntityManager entityManager;

    public AnalyticsController(UserRepository userRepository, CuratedListRepository listRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.entityManager = entityManager;
    }

    @GetMapping
    public String viewAnalytics(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        User user = userRepository.findByEmail(oauthUser.getAttribute("email")).orElseThrow();

        // Get all lists created by this verified creator
        List<CuratedList> userLists = listRepository.findByCreatorOrderByCreatedAtDesc(user);
        
        long totalLists = userLists.size();
        long totalViews = userLists.stream().mapToLong(CuratedList::getViewCount).sum();

        // Get total likes across all user's lists
        long totalLikes = 0;
        if (!userLists.isEmpty()) {
            totalLikes = (long) entityManager.createQuery("SELECT COUNT(l) FROM Like l WHERE l.list.creator = :user")
                    .setParameter("user", user)
                    .getSingleResult();
        }

        // Get total saves across all user's lists
        long totalSaves = 0;
        if (!userLists.isEmpty()) {
            totalSaves = (long) entityManager.createQuery("SELECT COUNT(sl) FROM SavedList sl WHERE sl.list.creator = :user")
                    .setParameter("user", user)
                    .getSingleResult();
        }

        // Social Metrics
        long followersCount = (long) entityManager.createQuery("SELECT COUNT(f) FROM Follow f WHERE f.followee = :user")
                .setParameter("user", user)
                .getSingleResult();

        long followingCount = (long) entityManager.createQuery("SELECT COUNT(f) FROM Follow f WHERE f.follower = :user")
                .setParameter("user", user)
                .getSingleResult();

        // Build list-by-list data for chart representation
        List<Map<String, Object>> listData = new ArrayList<>();
        Map<String, Long> categoryCount = new HashMap<>();

        for (CuratedList list : userLists) {
            long likes = (long) entityManager.createQuery("SELECT COUNT(l) FROM Like l WHERE l.list = :list")
                    .setParameter("list", list)
                    .getSingleResult();
            long saves = (long) entityManager.createQuery("SELECT COUNT(sl) FROM SavedList sl WHERE sl.list = :list")
                    .setParameter("list", list)
                    .getSingleResult();

            Map<String, Object> data = new HashMap<>();
            data.put("listId", list.getListId());
            data.put("title", list.getTitle());
            data.put("views", list.getViewCount());
            data.put("likes", likes);
            data.put("saves", saves);
            listData.add(data);

            String catName = list.getCategory().getName();
            categoryCount.put(catName, categoryCount.getOrDefault(catName, 0L) + 1);
        }

        model.addAttribute("totalLists", totalLists);
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("totalLikes", totalLikes);
        model.addAttribute("totalSaves", totalSaves);
        model.addAttribute("followersCount", followersCount);
        model.addAttribute("followingCount", followingCount);
        model.addAttribute("listData", listData);
        model.addAttribute("categoryData", categoryCount);

        return "analytics";
    }
}

package com.listitup.api.controller;

import com.listitup.api.model.AnalyticsSnapshot;
import com.listitup.api.repository.AnalyticsSnapshotRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import com.listitup.api.model.ListAnalytics;
import com.listitup.api.model.User;
import com.listitup.api.repository.ListAnalyticsRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Controller
@RequestMapping("/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'VERIFIED')")
public class AnalyticsController {

    private final AnalyticsSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final ListAnalyticsRepository listAnalyticsRepository;

    public AnalyticsController(AnalyticsSnapshotRepository snapshotRepository, UserRepository userRepository, ListAnalyticsRepository listAnalyticsRepository) {
        this.snapshotRepository = snapshotRepository;
        this.userRepository = userRepository;
        this.listAnalyticsRepository = listAnalyticsRepository;
    }

    @GetMapping
    public String viewAnalytics(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        User user = userRepository.findByEmail(oauthUser.getAttribute("email")).orElseThrow();

        if ("ADMIN".equals(user.getRole())) {
            List<AnalyticsSnapshot> snapshots = snapshotRepository.findAllByOrderBySnapshotDateAsc();
            model.addAttribute("snapshots", snapshots);
            model.addAttribute("isAdmin", true);
        } else {
            List<ListAnalytics> creatorAnalytics = listAnalyticsRepository.findByList_Creator(user);
            model.addAttribute("creatorAnalytics", creatorAnalytics);
            model.addAttribute("isAdmin", false);
            
            long totalViews = creatorAnalytics.stream().mapToLong(ListAnalytics::getViews).sum();
            long totalSaves = creatorAnalytics.stream().mapToLong(ListAnalytics::getSaves).sum();
            long totalLinkClicks = creatorAnalytics.stream().mapToLong(ListAnalytics::getLinkClicks).sum();
            
            model.addAttribute("totalViews", totalViews);
            model.addAttribute("totalSaves", totalSaves);
            model.addAttribute("totalLinkClicks", totalLinkClicks);
        }
        
        return "analytics";
    }
}

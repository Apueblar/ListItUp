package com.listitup.api.controller;

import com.listitup.api.model.AnalyticsSnapshot;
import com.listitup.api.repository.AnalyticsSnapshotRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'VERIFIED')")
public class AnalyticsController {

    private final AnalyticsSnapshotRepository snapshotRepository;

    public AnalyticsController(AnalyticsSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @GetMapping
    public String viewAnalytics(Model model) {
        List<AnalyticsSnapshot> snapshots = snapshotRepository.findAllByOrderBySnapshotDateAsc();
        model.addAttribute("snapshots", snapshots);
        return "analytics";
    }
}

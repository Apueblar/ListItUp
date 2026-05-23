package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Report;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.ReportRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/lists")
public class ReportController {

    private final ReportRepository reportRepository;
    private final CuratedListRepository listRepository;
    private final UserRepository userRepository;

    public ReportController(ReportRepository reportRepository, CuratedListRepository listRepository, UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.listRepository = listRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportList(@PathVariable UUID id, 
                                        @RequestBody Map<String, String> payload,
                                        @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        String email = oauthUser.getAttribute("email");
        User reporter = userRepository.findByEmail(email).orElseThrow();
        Optional<CuratedList> listOpt = listRepository.findById(id);
        
        if (listOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        String reason = payload.get("reason");
        String details = payload.get("details");
        
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
        }

        Report report = new Report();
        report.setList(listOpt.get());
        report.setReporter(reporter);
        report.setReason(reason);
        report.setDetails(details);
        
        reportRepository.save(report);

        return ResponseEntity.ok(Map.of("status", "reported"));
    }
}

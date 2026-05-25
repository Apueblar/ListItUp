package com.listitup.api.controller;

import com.listitup.api.model.Comment;
import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Item;
import com.listitup.api.model.Report;
import com.listitup.api.model.User;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.ItemRepository;
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
@RequestMapping("/reports")
public class ReportController {

    private final ReportRepository reportRepository;
    private final CuratedListRepository listRepository;
    private final ItemRepository itemRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReportController(ReportRepository reportRepository, CuratedListRepository listRepository,
                            ItemRepository itemRepository, CommentRepository commentRepository,
                            UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> submitReport(@RequestBody Map<String, String> payload,
                                          @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        String email = oauthUser.getAttribute("email");
        User reporter = userRepository.findByEmail(email).orElseThrow();
        
        String targetListId = payload.get("targetListId");
        String targetItemId = payload.get("targetItemId");
        String targetCommentId = payload.get("targetCommentId");
        String reason = payload.get("reason");
        String details = payload.get("details");
        
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
        }

        Report report = new Report();
        report.setSubmittedByUser(reporter);
        report.setReason(reason);
        report.setDetails(details);
        report.setStatus("OPEN");

        int targets = 0;
        if (targetListId != null && !targetListId.isBlank()) {
            Optional<CuratedList> listOpt = listRepository.findById(UUID.fromString(targetListId));
            if (listOpt.isPresent()) {
                report.setTargetList(listOpt.get());
                targets++;
            }
        }
        if (targetItemId != null && !targetItemId.isBlank()) {
            Optional<Item> itemOpt = itemRepository.findById(UUID.fromString(targetItemId));
            if (itemOpt.isPresent()) {
                report.setTargetItem(itemOpt.get());
                targets++;
            }
        }
        if (targetCommentId != null && !targetCommentId.isBlank()) {
            Optional<Comment> commentOpt = commentRepository.findById(UUID.fromString(targetCommentId));
            if (commentOpt.isPresent()) {
                report.setTargetComment(commentOpt.get());
                targets++;
            }
        }

        if (targets != 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Exactly one valid target ID must be provided"));
        }
        
        reportRepository.save(report);

        return ResponseEntity.ok(Map.of("status", "reported"));
    }
}


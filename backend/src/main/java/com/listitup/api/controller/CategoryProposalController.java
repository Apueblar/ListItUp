package com.listitup.api.controller;

import com.listitup.api.model.CategoryProposal;
import com.listitup.api.model.User;
import com.listitup.api.repository.CategoryProposalRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/categories/propose")
public class CategoryProposalController {

    private final CategoryProposalRepository proposalRepository;
    private final UserRepository userRepository;

    public CategoryProposalController(CategoryProposalRepository proposalRepository, UserRepository userRepository) {
        this.proposalRepository = proposalRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> proposeCategory(@RequestParam String proposedName, @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) return ResponseEntity.status(401).build();
        
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        if (proposedName == null || proposedName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category name cannot be empty"));
        }

        CategoryProposal proposal = new CategoryProposal();
        proposal.setProposer(user);
        proposal.setProposedName(proposedName.trim());
        proposalRepository.save(proposal);

        return ResponseEntity.ok(Map.of("message", "Proposal submitted successfully and is pending admin approval."));
    }
}

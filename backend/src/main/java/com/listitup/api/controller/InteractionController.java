package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Like;
import com.listitup.api.model.SavedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@RestController
public class InteractionController {

    private final CuratedListRepository listRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public InteractionController(CuratedListRepository listRepository, UserRepository userRepository, EntityManager entityManager) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @PostMapping("/lists/{id}/like")
    @Transactional
    public ResponseEntity<?> toggleLike(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listRepository.findById(id).orElseThrow();

        // Simple toggle logic (in a real app, use a LikeRepository)
        long count = (long) entityManager.createQuery("SELECT COUNT(l) FROM Like l WHERE l.user = :u AND l.list = :list")
                .setParameter("u", user)
                .setParameter("list", list)
                .getSingleResult();

        if (count > 0) {
            entityManager.createQuery("DELETE FROM Like l WHERE l.user = :u AND l.list = :list")
                    .setParameter("u", user)
                    .setParameter("list", list)
                    .executeUpdate();
            return ResponseEntity.ok(Map.of("status", "unliked"));
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setList(list);
            entityManager.persist(like);
            return ResponseEntity.ok(Map.of("status", "liked"));
        }
    }

    @PostMapping("/lists/{id}/save")
    @Transactional
    public ResponseEntity<?> toggleSave(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listRepository.findById(id).orElseThrow();

        long count = (long) entityManager.createQuery("SELECT COUNT(s) FROM SavedList s WHERE s.user = :u AND s.list = :list")
                .setParameter("u", user)
                .setParameter("list", list)
                .getSingleResult();

        if (count > 0) {
            entityManager.createQuery("DELETE FROM SavedList s WHERE s.user = :u AND s.list = :list")
                    .setParameter("u", user)
                    .setParameter("list", list)
                    .executeUpdate();
            return ResponseEntity.ok(Map.of("status", "unsaved"));
        } else {
            SavedList savedList = new SavedList();
            savedList.setUser(user);
            savedList.setList(list);
            entityManager.persist(savedList);
            return ResponseEntity.ok(Map.of("status", "saved"));
        }
    }
}

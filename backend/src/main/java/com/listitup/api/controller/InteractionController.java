package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Like;
import com.listitup.api.model.SavedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.NotificationRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.EmailService;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class InteractionController {

    private final CuratedListRepository listRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;
    private final EmailService emailService;

    public InteractionController(CuratedListRepository listRepository, UserRepository userRepository, NotificationRepository notificationRepository, EntityManager entityManager, EmailService emailService) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
        this.emailService = emailService;
    }

    @PostMapping("/lists/{id}/like")
    @Transactional
    public ResponseEntity<?> toggleLike(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();
        CuratedList list = listRepository.findById(id).orElseThrow();

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

            if (!user.getUserId().equals(list.getCreator().getUserId())) {
                // Delete any existing like notification from this user for this list
                // so re-liking after unliking replaces it instead of stacking a duplicate
                String likeLink = "/lists/" + list.getListId();
                String likeMsg = "❤️ " + user.getUsername() + " liked your list: " + list.getTitle();
                entityManager.createQuery(
                        "DELETE FROM Notification n WHERE n.user = :owner AND n.linkUrl = :link AND n.message = :msg")
                        .setParameter("owner", list.getCreator())
                        .setParameter("link", likeLink)
                        .setParameter("msg", likeMsg)
                        .executeUpdate();

                com.listitup.api.model.Notification n = new com.listitup.api.model.Notification();
                n.setUser(list.getCreator());
                n.setMessage(likeMsg);
                n.setLinkUrl(likeLink);
                notificationRepository.save(n);
                
                // Send email
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    emailService.sendEmail(list.getCreator().getEmail(), "Someone liked your list", likeMsg + "\n\nView it here: https://listitup.duckdns.org" + likeLink);
                });
            }

            return ResponseEntity.ok(Map.of("status", "liked"));
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/lists/{id}/likes")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, Object>> getLikes(@PathVariable UUID id) {
        CuratedList list = listRepository.findById(id).orElseThrow();
        java.util.List<Like> likes = entityManager.createQuery(
                "SELECT l FROM Like l WHERE l.list = :list ORDER BY l.createdAt DESC", Like.class)
                .setParameter("list", list)
                .getResultList();

        return likes.stream().map(l -> {
            User u = l.getUser();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("userId", u.getUserId());
            map.put("username", u.getUsername());
            map.put("profilePicture", u.getProfilePicture() == null ? "" : u.getProfilePicture());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/lists/{id}/save")
    @Transactional
    public ResponseEntity<?> toggleSave(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();
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

    @PostMapping("/lists/{id}/items/{itemId}/click")
    @Transactional
    public ResponseEntity<?> trackItemClick(@PathVariable UUID id, @PathVariable UUID itemId) {
        int updated = entityManager.createQuery("UPDATE Item i SET i.clickCount = i.clickCount + 1 WHERE i.itemId = :itemId AND i.list.listId = :listId")
                .setParameter("itemId", itemId)
                .setParameter("listId", id)
                .executeUpdate();
        if (updated > 0) {
            return ResponseEntity.ok(Map.of("status", "tracked"));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/lists/{id}/pin")
    @Transactional
    public ResponseEntity<?> togglePin(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();
        CuratedList list = listRepository.findById(id).orElseThrow();

        // Only the creator can pin/unpin their list
        if (!list.getCreator().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: You are not the owner of this list"));
        }

        // Only verified creators can pin lists
        if (!Boolean.TRUE.equals(user.getCanPinLists())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Only verified creators can pin lists"));
        }

        boolean nextPin = !Boolean.TRUE.equals(list.getIsPinned());
        list.setIsPinned(nextPin);
        listRepository.save(list);

        return ResponseEntity.ok(Map.of("status", nextPin ? "pinned" : "unpinned"));
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/users/{username}/follow", method = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.DELETE})
    @Transactional
    public ResponseEntity<?> toggleFollow(@PathVariable String username, @AuthenticationPrincipal OAuth2User oauthUser, jakarta.servlet.http.HttpServletRequest request) {
        if (oauthUser == null) return ResponseEntity.status(401).build();
        User follower = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        User followee = userRepository.findFirstByUsername(username).orElseThrow();

        if (follower.getUserId().equals(followee.getUserId())) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getMethod().equalsIgnoreCase("DELETE")) {
            entityManager.createQuery("DELETE FROM Follow f WHERE f.follower = :follower AND f.followee = :followee")
                    .setParameter("follower", follower)
                    .setParameter("followee", followee)
                    .executeUpdate();
            return ResponseEntity.ok(Map.of("status", "unfollowed"));
        } else {
            long count = (long) entityManager.createQuery("SELECT COUNT(f) FROM Follow f WHERE f.follower = :follower AND f.followee = :followee")
                    .setParameter("follower", follower)
                    .setParameter("followee", followee)
                    .getSingleResult();
            if (count == 0) {
                com.listitup.api.model.Follow follow = new com.listitup.api.model.Follow();
                follow.setFollower(follower);
                follow.setFollowee(followee);
                entityManager.persist(follow);

                // Delete any existing follow notification from this follower to this followee
                // so re-following after unfollowing replaces it instead of stacking a duplicate
                String followLink = "/users/" + follower.getUsername();
                String followMsg = "👋 " + follower.getUsername() + " started following you!";
                entityManager.createQuery(
                        "DELETE FROM Notification n WHERE n.user = :followee AND n.linkUrl = :link AND n.message = :msg")
                        .setParameter("followee", followee)
                        .setParameter("link", followLink)
                        .setParameter("msg", followMsg)
                        .executeUpdate();

                com.listitup.api.model.Notification n = new com.listitup.api.model.Notification();
                n.setUser(followee);
                n.setMessage(followMsg);
                n.setLinkUrl(followLink);
                notificationRepository.save(n);
                
                // Send email
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    emailService.sendEmail(followee.getEmail(), "Someone followed you", followMsg + "\n\nView it here: https://listitup.duckdns.org" + followLink);
                });
            }
            return ResponseEntity.ok(Map.of("status", "followed"));
        }
    }
}

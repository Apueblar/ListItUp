package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.SavedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.SavedListRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.CuratedListService;
import com.listitup.api.model.Follow;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final CuratedListRepository listRepository;
    private final SavedListRepository savedListRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final CuratedListService listService;

    public ProfileController(UserRepository userRepository, CuratedListRepository listRepository, SavedListRepository savedListRepository, jakarta.persistence.EntityManager entityManager, CuratedListService listService) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.savedListRepository = savedListRepository;
        this.entityManager = entityManager;
        this.listService = listService;
    }

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();

        // Sort by Pinned status first
        List<CuratedList> myLists = listRepository.findByCreatorOrderByIsPinnedDescCreatedAtDesc(user);
        
        List<CuratedList> savedLists = savedListRepository.findByUserOrderBySavedAtDesc(user)
                .stream()
                .map(SavedList::getList)
                .collect(Collectors.toList());

        List<CuratedList> likedLists = entityManager.createQuery(
                "SELECT l.list FROM Like l WHERE l.user = :user ORDER BY l.createdAt DESC", CuratedList.class)
                .setParameter("user", user)
                .getResultList();

        long followersCount = (long) entityManager.createQuery("SELECT COUNT(uf) FROM Follow uf WHERE uf.followee = :user")
                .setParameter("user", user).getSingleResult();
        long followingCount = (long) entityManager.createQuery("SELECT COUNT(uf) FROM Follow uf WHERE uf.follower = :user")
                .setParameter("user", user).getSingleResult();

        model.addAttribute("user", user);
        model.addAttribute("myLists", myLists);
        model.addAttribute("savedLists", savedLists);
        model.addAttribute("likedLists", likedLists);
        model.addAttribute("followersCount", followersCount);
        model.addAttribute("followingCount", followingCount);

        return "profile";
    }

    @GetMapping("/users/{username}")
    public String viewPublicProfile(@PathVariable String username, Model model, @AuthenticationPrincipal OAuth2User oauthUser) {
        java.util.Optional<User> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/feed?userNotFound=true";
        }
        User user = userOpt.get();

        List<CuratedList> myLists;
        boolean isOwner = false;
        if (oauthUser != null) {
            String email = oauthUser.getAttribute("email");
            java.util.Optional<User> currentUserOpt = userRepository.findFirstByEmail(email);
            if (currentUserOpt.isPresent() && currentUserOpt.get().getUserId().equals(user.getUserId())) {
                isOwner = true;
            }
        }

        if (isOwner) {
            myLists = listRepository.findByCreatorOrderByIsPinnedDescCreatedAtDesc(user);
        } else {
            myLists = listRepository.findByCreatorAndIsDraftFalseOrderByIsPinnedDescCreatedAtDesc(user);
        }

        long followersCount = (long) entityManager.createQuery("SELECT COUNT(uf) FROM Follow uf WHERE uf.followee = :user")
                .setParameter("user", user).getSingleResult();
        long followingCount = (long) entityManager.createQuery("SELECT COUNT(uf) FROM Follow uf WHERE uf.follower = :user")
                .setParameter("user", user).getSingleResult();

        boolean isFollowing = false;
        if (oauthUser != null) {
            String email = oauthUser.getAttribute("email");
            java.util.Optional<User> currentUserOpt = userRepository.findFirstByEmail(email);
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                model.addAttribute("currentUser", currentUser);
                
                if (!currentUser.getUserId().equals(user.getUserId())) {
                    long followCount = (long) entityManager.createQuery("SELECT COUNT(uf) FROM Follow uf WHERE uf.follower = :follower AND uf.followee = :followee")
                            .setParameter("follower", currentUser)
                            .setParameter("followee", user)
                            .getSingleResult();
                    isFollowing = followCount > 0;
                }
            }
        }

        model.addAttribute("profileUser", user);
        model.addAttribute("myLists", myLists);
        model.addAttribute("followersCount", followersCount);
        model.addAttribute("followingCount", followingCount);
        model.addAttribute("isFollowing", isFollowing);

        return "user-profile";
    }

    @PostMapping("/profile/update-username")
    public String updateUsername(@AuthenticationPrincipal OAuth2User oauthUser, 
                                 @RequestParam String username,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (oauthUser == null) {
            return "redirect:/feed";
        }
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();

        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("usernameError", "Username cannot be empty.");
            return "redirect:/profile";
        }

        String newUsername = username.trim();

        // Enforce no spaces and pattern match
        if (newUsername.contains(" ") || !newUsername.matches("^[a-zA-Z0-9_.]+$")) {
            redirectAttributes.addFlashAttribute("usernameError", "Username must not contain spaces and can only include alphanumeric characters, underscores, and periods.");
            return "redirect:/profile";
        }

        // Check if username is already in use (application-level check)
        java.util.Optional<User> existingUser = userRepository.findFirstByUsername(newUsername);
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("usernameError", "Username '" + newUsername + "' is already taken. Please choose another.");
            return "redirect:/profile";
        }

        try {
            user.setUsername(newUsername);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("usernameSuccess", "Username successfully updated to " + newUsername + "!");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("usernameError", "Username '" + newUsername + "' is already taken. Please choose another.");
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/update-avatar")
    public String updateAvatar(@AuthenticationPrincipal OAuth2User oauthUser, 
                               @RequestParam String profilePicture,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (oauthUser == null) return "redirect:/feed";
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        
        String url = profilePicture.trim();
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/uploads/")) {
            redirectAttributes.addFlashAttribute("profileError", "Invalid profile picture URL.");
            return "redirect:/profile";
        }
        
        user.setProfilePicture(url);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("profileSuccess", "Profile picture updated successfully!");
        return "redirect:/profile";
    }

    @PostMapping("/profile/update-bio")
    public String updateBio(@AuthenticationPrincipal OAuth2User oauthUser, 
                            @RequestParam String biography,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (oauthUser == null) return "redirect:/feed";
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        
        user.setBiography(biography.trim());
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("profileSuccess", "Bio updated successfully!");
        return "redirect:/profile";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(@AuthenticationPrincipal OAuth2User oauthUser, jakarta.servlet.http.HttpServletRequest request) throws jakarta.servlet.ServletException {
        if (oauthUser == null) return "redirect:/feed";
        User user = userRepository.findFirstByEmail(oauthUser.getAttribute("email")).orElseThrow();
        
        // Ensure main admin cannot be deleted this way to prevent lockouts
        if (user.getEmail().equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
            return "redirect:/profile?error=mainAdminCannotBeDeleted";
        }

        listService.deleteUser(user.getUserId());
        request.logout();
        return "redirect:/";
    }

    @GetMapping("/setup-username")
    public String showSetupUsername(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        if (oauthUser == null) {
            return "redirect:/";
        }
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();

        if (Boolean.TRUE.equals(user.getHasCompletedSetup())) {
            return "redirect:/feed";
        }

        // Derive clean suggested username (remove spaces/invalid chars)
        String suggested = user.getUsername();
        if (suggested != null) {
            suggested = suggested.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9_.]", "");
        } else {
            suggested = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.]", "");
        }

        model.addAttribute("suggestedUsername", suggested);
        model.addAttribute("placeholderUsername", suggested);
        return "setup-username";
    }

    @PostMapping("/setup-username")
    public String setupUsername(@AuthenticationPrincipal OAuth2User oauthUser, 
                                @RequestParam String username, 
                                Model model) {
        if (oauthUser == null) {
            return "redirect:/";
        }
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findFirstByEmail(email).orElseThrow();

        if (Boolean.TRUE.equals(user.getHasCompletedSetup())) {
            return "redirect:/feed";
        }

        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Username cannot be empty.");
            model.addAttribute("suggestedUsername", username);
            return "setup-username";
        }

        String cleanedUsername = username.trim();

        if (cleanedUsername.contains(" ") || !cleanedUsername.matches("^[a-zA-Z0-9_.]+$")) {
            model.addAttribute("error", "Username must not contain spaces and can only include alphanumeric characters, underscores, and periods.");
            model.addAttribute("suggestedUsername", cleanedUsername);
            return "setup-username";
        }

        java.util.Optional<User> existingUser = userRepository.findFirstByUsername(cleanedUsername);
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(user.getUserId())) {
            model.addAttribute("error", "Username '" + cleanedUsername + "' is already taken. Please try another.");
            model.addAttribute("suggestedUsername", cleanedUsername);
            return "setup-username";
        }

        try {
            user.setUsername(cleanedUsername);
            user.setHasCompletedSetup(true);
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Username '" + cleanedUsername + "' is already taken. Please try another.");
            model.addAttribute("suggestedUsername", cleanedUsername);
            return "setup-username";
        }

        return "redirect:/feed";
    }

    @GetMapping("/users/{username}/followers")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, Object>> getFollowers(@PathVariable String username) {
        User user = userRepository.findFirstByUsername(username).orElseThrow();
        java.util.List<Follow> follows = entityManager.createQuery(
                "SELECT f FROM Follow f WHERE f.followee = :user ORDER BY f.createdAt DESC", Follow.class)
                .setParameter("user", user)
                .getResultList();
        
        return follows.stream().map(f -> {
            User follower = f.getFollower();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("userId", follower.getUserId());
            map.put("username", follower.getUsername());
            map.put("profilePicture", follower.getProfilePicture() == null ? "" : follower.getProfilePicture());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/users/{username}/following")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, Object>> getFollowing(@PathVariable String username) {
        User user = userRepository.findFirstByUsername(username).orElseThrow();
        java.util.List<Follow> follows = entityManager.createQuery(
                "SELECT f FROM Follow f WHERE f.follower = :user ORDER BY f.createdAt DESC", Follow.class)
                .setParameter("user", user)
                .getResultList();
        
        return follows.stream().map(f -> {
            User followee = f.getFollowee();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("userId", followee.getUserId());
            map.put("username", followee.getUsername());
            map.put("profilePicture", followee.getProfilePicture() == null ? "" : followee.getProfilePicture());
            return map;
        }).collect(Collectors.toList());
    }
}


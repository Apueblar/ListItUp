package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.SavedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.SavedListRepository;
import com.listitup.api.repository.UserRepository;
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

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final CuratedListRepository listRepository;
    private final SavedListRepository savedListRepository;

    public ProfileController(UserRepository userRepository, CuratedListRepository listRepository, SavedListRepository savedListRepository) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.savedListRepository = savedListRepository;
    }

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();

        // Sort by Pinned status first
        List<CuratedList> myLists = listRepository.findByCreatorOrderByIsPinnedDescCreatedAtDesc(user);
        
        List<CuratedList> savedLists = savedListRepository.findByUserOrderBySavedAtDesc(user)
                .stream()
                .map(SavedList::getList)
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("myLists", myLists);
        model.addAttribute("savedLists", savedLists);

        return "profile";
    }

    @GetMapping("/users/{username}")
    public String viewPublicProfile(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Sort by Pinned status first
        List<CuratedList> myLists = listRepository.findByCreatorOrderByIsPinnedDescCreatedAtDesc(user);

        model.addAttribute("profileUser", user);
        model.addAttribute("myLists", myLists);

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
        User user = userRepository.findByEmail(email).orElseThrow();

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

        // Check if username is already in use
        java.util.Optional<User> existingUser = userRepository.findByUsername(newUsername);
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("usernameError", "Username is already in use!");
            return "redirect:/profile";
        }

        user.setUsername(newUsername);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("usernameSuccess", "Username successfully updated to " + newUsername + "!");
        return "redirect:/profile";
    }

    @GetMapping("/setup-username")
    public String showSetupUsername(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        if (oauthUser == null) {
            return "redirect:/";
        }
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();

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
        User user = userRepository.findByEmail(email).orElseThrow();

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

        java.util.Optional<User> existingUser = userRepository.findByUsername(cleanedUsername);
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(user.getUserId())) {
            model.addAttribute("error", "Username is already in use! Please try another one.");
            model.addAttribute("suggestedUsername", cleanedUsername);
            return "setup-username";
        }

        user.setUsername(cleanedUsername);
        user.setHasCompletedSetup(true);
        userRepository.save(user);

        return "redirect:/feed";
    }
}

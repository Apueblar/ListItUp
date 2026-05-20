package com.listitup.api.controller;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/admin")
    public String adminPanel(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin"; // renders admin.html
    }

    @PostMapping("/admin/users/role")
    public String updateUserRole(@RequestParam UUID userId, @RequestParam String role) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Restrict modifying the main admin's role to prevent lockouts
            if (!user.getEmail().equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
                user.setRole(role);
                syncPrivileges(user);
                userRepository.save(user);
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/toggle-block")
    public String toggleUserBlock(@RequestParam UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Restrict blocking the main admin
            if (!user.getEmail().equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
                user.setIsBlocked(!Boolean.TRUE.equals(user.getIsBlocked()));
                userRepository.save(user);
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/toggle-badge")
    public String toggleUserBadge(@RequestParam UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setHasBadge(!Boolean.TRUE.equals(user.getHasBadge()));
            // If they get verified, let's promote them to VERIFIED role as well if they are STANDARD
            if (Boolean.TRUE.equals(user.getHasBadge()) && "STANDARD".equals(user.getRole())) {
                user.setRole("VERIFIED");
            } else if (!Boolean.TRUE.equals(user.getHasBadge()) && "VERIFIED".equals(user.getRole())) {
                user.setRole("STANDARD");
            }
            syncPrivileges(user);
            userRepository.save(user);
        }
        return "redirect:/admin";
    }

    private void syncPrivileges(User user) {
        String role = user.getRole();
        if ("VERIFIED".equals(role)) {
            user.setHasBadge(true);
            user.setCanPinLists(true);
            user.setHasAnalyticsAccess(true);
            user.setCanModerateContent(false);
            user.setCanDeleteAny(false);
        } else if ("ADMIN".equals(role)) {
            user.setHasBadge(false);
            user.setCanPinLists(false);
            user.setHasAnalyticsAccess(false);
            user.setCanModerateContent(true);
            user.setCanDeleteAny(true);
        } else { // STANDARD
            user.setHasBadge(false);
            user.setCanPinLists(false);
            user.setHasAnalyticsAccess(false);
            user.setCanModerateContent(false);
            user.setCanDeleteAny(false);
        }
    }
}

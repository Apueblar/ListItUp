package com.listitup.api.service;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CuratedListService listService;

    public UserService(UserRepository userRepository, CuratedListService listService) {
        this.userRepository = userRepository;
        this.listService = listService;
    }

    @Transactional
    public User createUser(User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (userRepository.findFirstByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }
        if (user.getUsername() != null && userRepository.findFirstByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("Username already taken");
        }
        if (user.getRole() == null) {
            user.setRole("STANDARD");
        }
        return userRepository.save(user);
    }

    @Transactional
    public User assignRole(UUID userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(UUID userId, String username, String biography, String profilePicture) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        
        if (username != null && !username.trim().isEmpty()) {
            String cleanUsername = username.trim();
            if (cleanUsername.contains(" ") || !cleanUsername.matches("^[a-zA-Z0-9_.]+$")) {
                throw new IllegalArgumentException("Invalid username format");
            }
            Optional<User> existing = userRepository.findFirstByUsername(cleanUsername);
            if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
                throw new IllegalStateException("Username already taken");
            }
            user.setUsername(cleanUsername);
        }

        if (biography != null) {
            user.setBiography(biography.trim());
        }

        if (profilePicture != null) {
            user.setProfilePicture(profilePicture.trim());
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportGdprData(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("email", user.getEmail());
        data.put("username", user.getUsername());
        data.put("biography", user.getBiography());
        data.put("profilePicture", user.getProfilePicture());
        data.put("role", user.getRole());
        data.put("createdAt", user.getCreatedAt());
        data.put("authProvider", user.getAuthProvider());

        return data;
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        listService.deleteUser(userId);
    }
}

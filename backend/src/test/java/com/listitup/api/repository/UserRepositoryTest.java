package com.listitup.api.repository;

import com.listitup.api.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindById() {
        User user = new User();
        user.setEmail("repo@example.com");
        user.setUsername("repo_user");
        user.setRole("STANDARD");
        user.setAuthProvider("GOOGLE");

        User saved = userRepository.save(user);
        assertNotNull(saved.getUserId());

        Optional<User> found = userRepository.findById(saved.getUserId());
        assertTrue(found.isPresent());
        assertEquals("repo@example.com", found.get().getEmail());
    }

    @Test
    void testFindFirstByEmail_Found() {
        User user = new User();
        user.setEmail("email@example.com");
        user.setUsername("email_user");
        user.setRole("STANDARD");
        user.setAuthProvider("GOOGLE");
        userRepository.save(user);

        Optional<User> found = userRepository.findFirstByEmail("email@example.com");
        assertTrue(found.isPresent());
        assertEquals("email_user", found.get().getUsername());
    }

    @Test
    void testFindFirstByEmail_NotFound() {
        Optional<User> found = userRepository.findFirstByEmail("nonexistent@example.com");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindFirstByUsername_Found() {
        User user = new User();
        user.setEmail("user1@example.com");
        user.setUsername("unique_username");
        user.setRole("STANDARD");
        user.setAuthProvider("GOOGLE");
        userRepository.save(user);

        Optional<User> found = userRepository.findFirstByUsername("unique_username");
        assertTrue(found.isPresent());
        assertEquals("user1@example.com", found.get().getEmail());
    }

    @Test
    void testFindFirstByUsernameAndAuthProvider_Found() {
        User user = new User();
        user.setEmail("user2@example.com");
        user.setUsername("oauth_user");
        user.setAuthProvider("GITHUB");
        user.setRole("STANDARD");
        userRepository.save(user);

        Optional<User> found = userRepository.findFirstByUsernameAndAuthProvider("oauth_user", "GITHUB");
        assertTrue(found.isPresent());
        assertEquals("user2@example.com", found.get().getEmail());
    }
}

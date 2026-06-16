package com.listitup.api.service;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CuratedListService listService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole("STANDARD");
    }

    @Test
    void testCreateUser_Success() {
        when(userRepository.findFirstByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findFirstByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(testUser);

        User created = userService.createUser(testUser);
        assertNotNull(created);
        assertEquals("test@example.com", created.getEmail());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void testCreateUser_EmptyEmail() {
        testUser.setEmail("");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(testUser));
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        when(userRepository.findFirstByEmail(any())).thenReturn(Optional.of(testUser));
        assertThrows(IllegalStateException.class, () -> userService.createUser(testUser));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        when(userRepository.findFirstByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findFirstByUsername(any())).thenReturn(Optional.of(testUser));
        assertThrows(IllegalStateException.class, () -> userService.createUser(testUser));
    }

    @Test
    void testCreateUser_DefaultRole() {
        testUser.setRole(null);
        when(userRepository.findFirstByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findFirstByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(testUser);
        assertEquals("STANDARD", created.getRole());
    }

    @Test
    void testAssignRole_Success() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.assignRole(testUser.getUserId(), "ADMIN");
        assertEquals("ADMIN", updated.getRole());
    }

    @Test
    void testAssignRole_UserNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> userService.assignRole(UUID.randomUUID(), "ADMIN"));
    }

    @Test
    void testUpdateProfile_Success() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userRepository.findFirstByUsername("newname")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateProfile(testUser.getUserId(), "newname", "My new bio", "new-avatar.jpg");
        assertEquals("newname", updated.getUsername());
        assertEquals("My new bio", updated.getBiography());
        assertEquals("new-avatar.jpg", updated.getProfilePicture());
    }

    @Test
    void testUpdateProfile_InvalidUsernameFormat() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        assertThrows(IllegalArgumentException.class, () -> 
            userService.updateProfile(testUser.getUserId(), "invalid name!", null, null)
        );
    }

    @Test
    void testUpdateProfile_UsernameTaken() {
        User otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setUsername("takenname");

        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(userRepository.findFirstByUsername("takenname")).thenReturn(Optional.of(otherUser));

        assertThrows(IllegalStateException.class, () -> 
            userService.updateProfile(testUser.getUserId(), "takenname", null, null)
        );
    }

    @Test
    void testExportGdprData() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        Map<String, Object> export = userService.exportGdprData(testUser.getUserId());
        assertNotNull(export);
        assertEquals(testUser.getUserId(), export.get("userId"));
        assertEquals("test@example.com", export.get("email"));
        assertEquals("testuser", export.get("username"));
    }

    @Test
    void testDeleteAccount() {
        doNothing().when(listService).deleteUser(testUser.getUserId());
        userService.deleteAccount(testUser.getUserId());
        verify(listService, times(1)).deleteUser(testUser.getUserId());
    }
}

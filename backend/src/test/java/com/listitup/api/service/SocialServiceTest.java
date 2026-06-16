package com.listitup.api.service;

import com.listitup.api.model.*;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.NotificationRepository;
import com.listitup.api.repository.SavedListRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
public class SocialServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private SavedListRepository savedListRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SocialService socialService;

    private User follower;
    private User followee;
    private CuratedList list;
    private Comment comment;

    @BeforeEach
    void setUp() {
        follower = new User();
        follower.setUserId(UUID.randomUUID());
        follower.setUsername("follower");

        followee = new User();
        followee.setUserId(UUID.randomUUID());
        followee.setUsername("followee");
        followee.setEmail("followee@example.com");

        list = new CuratedList();
        list.setListId(UUID.randomUUID());
        list.setTitle("Curated List");
        list.setCreator(followee);

        comment = new Comment();
        comment.setCommentId(UUID.randomUUID());
        comment.setAuthor(follower);
        comment.setList(list);
        comment.setText("Great list!");
    }

    @Test
    void testFollowUser_Success() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);
        doNothing().when(entityManager).persist(any(Follow.class));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        socialService.followUser(follower, followee);
        verify(entityManager, times(1)).persist(any(Follow.class));
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void testFollowUser_AlreadyFollowing() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(1L);

        socialService.followUser(follower, followee);
        verify(entityManager, never()).persist(any(Follow.class));
    }

    @Test
    void testFollowUser_SelfFollowForbidden() {
        assertThrows(IllegalArgumentException.class, () -> socialService.followUser(follower, follower));
    }

    @Test
    void testUnfollowUser() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(1);

        socialService.unfollowUser(follower, followee);
        verify(entityManager, times(1)).createQuery(anyString());
    }

    @Test
    void testToggleLike_LikeSuccess() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);
        doNothing().when(entityManager).persist(any(Like.class));

        boolean status = socialService.toggleLike(follower, list);
        assertTrue(status);
        verify(entityManager, times(1)).persist(any(Like.class));
    }

    @Test
    void testToggleLike_UnlikeSuccess() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(1L);

        boolean status = socialService.toggleLike(follower, list);
        assertFalse(status);
        verify(mockQuery, times(1)).executeUpdate();
    }

    @Test
    void testToggleSave_SaveSuccess() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);
        doNothing().when(entityManager).persist(any(SavedList.class));

        boolean status = socialService.toggleSave(follower, list);
        assertTrue(status);
        verify(entityManager, times(1)).persist(any(SavedList.class));
    }

    @Test
    void testToggleSave_UnsaveSuccess() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(1L);

        boolean status = socialService.toggleSave(follower, list);
        assertFalse(status);
        verify(mockQuery, times(1)).executeUpdate();
    }

    @Test
    void testAddComment_Success() {
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Comment saved = socialService.addComment(follower, list, "Great list!");
        assertNotNull(saved);
        assertEquals("Great list!", saved.getText());
        verify(commentRepository, times(1)).save(any());
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void testAddComment_EmptyText() {
        assertThrows(IllegalArgumentException.class, () -> socialService.addComment(follower, list, ""));
    }

    @Test
    void testGetCommentsByList() {
        when(commentRepository.findByListOrderByCreatedAtDesc(list)).thenReturn(List.of(comment));
        List<Comment> comments = socialService.getCommentsByList(list);
        assertEquals(1, comments.size());
    }

    @Test
    void testDeleteComment_AuthorSuccess() {
        when(commentRepository.findById(comment.getCommentId())).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(comment);

        socialService.deleteComment(comment.getCommentId(), follower);
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void testDeleteComment_Unauthorized() {
        User randomUser = new User();
        randomUser.setUserId(UUID.randomUUID());
        randomUser.setRole("STANDARD");

        when(commentRepository.findById(comment.getCommentId())).thenReturn(Optional.of(comment));
        assertThrows(SecurityException.class, () -> socialService.deleteComment(comment.getCommentId(), randomUser));
    }

    @Test
    void testTriggerNotification() {
        Notification notification = new Notification();
        notification.setMessage("Test message");
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Notification result = socialService.triggerNotification(followee, "Test message", "TEST", "/test");
        assertNotNull(result);
        assertEquals("Test message", result.getMessage());
    }
}

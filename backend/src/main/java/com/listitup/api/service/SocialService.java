package com.listitup.api.service;

import com.listitup.api.model.*;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.NotificationRepository;
import com.listitup.api.repository.SavedListRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SocialService {

    private final CommentRepository commentRepository;
    private final SavedListRepository savedListRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    public SocialService(CommentRepository commentRepository,
                         SavedListRepository savedListRepository,
                         NotificationRepository notificationRepository,
                         EntityManager entityManager) {
        this.commentRepository = commentRepository;
        this.savedListRepository = savedListRepository;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void followUser(User follower, User followee) {
        if (follower.getUserId().equals(followee.getUserId())) {
            throw new IllegalArgumentException("Cannot follow oneself");
        }

        long count = (long) entityManager.createQuery("SELECT COUNT(f) FROM Follow f WHERE f.follower = :follower AND f.followee = :followee")
                .setParameter("follower", follower)
                .setParameter("followee", followee)
                .getSingleResult();

        if (count == 0) {
            Follow follow = new Follow();
            follow.setFollower(follower);
            follow.setFollowee(followee);
            entityManager.persist(follow);

            triggerNotification(followee, "👋 " + follower.getUsername() + " started following you!", "FOLLOW", "/users/" + follower.getUsername());
        }
    }

    @Transactional
    public void unfollowUser(User follower, User followee) {
        entityManager.createQuery("DELETE FROM Follow f WHERE f.follower = :follower AND f.followee = :followee")
                .setParameter("follower", follower)
                .setParameter("followee", followee)
                .executeUpdate();
    }

    @Transactional
    public boolean toggleLike(User user, CuratedList list) {
        long count = (long) entityManager.createQuery("SELECT COUNT(l) FROM Like l WHERE l.user = :u AND l.list = :list")
                .setParameter("u", user)
                .setParameter("list", list)
                .getSingleResult();

        if (count > 0) {
            entityManager.createQuery("DELETE FROM Like l WHERE l.user = :u AND l.list = :list")
                    .setParameter("u", user)
                    .setParameter("list", list)
                    .executeUpdate();
            return false;
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setList(list);
            entityManager.persist(like);

            if (!user.getUserId().equals(list.getCreator().getUserId())) {
                triggerNotification(list.getCreator(), "❤️ " + user.getUsername() + " liked your list: " + list.getTitle(), "LIKE", "/lists/" + list.getListId());
            }
            return true;
        }
    }

    @Transactional
    public boolean toggleSave(User user, CuratedList list) {
        long count = (long) entityManager.createQuery("SELECT COUNT(s) FROM SavedList s WHERE s.user = :u AND s.list = :list")
                .setParameter("u", user)
                .setParameter("list", list)
                .getSingleResult();

        if (count > 0) {
            entityManager.createQuery("DELETE FROM SavedList s WHERE s.user = :u AND s.list = :list")
                    .setParameter("u", user)
                    .setParameter("list", list)
                    .executeUpdate();
            return false;
        } else {
            SavedList savedList = new SavedList();
            savedList.setUser(user);
            savedList.setList(list);
            entityManager.persist(savedList);
            return true;
        }
    }

    @Transactional
    public Comment addComment(User author, CuratedList list, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setList(list);
        comment.setText(text.trim());
        Comment savedComment = commentRepository.save(comment);

        if (!author.getUserId().equals(list.getCreator().getUserId())) {
            triggerNotification(list.getCreator(), "💬 " + author.getUsername() + " commented on your list: " + list.getTitle(), "COMMENT", "/lists/" + list.getListId());
        }
        return savedComment;
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByList(CuratedList list) {
        return commentRepository.findByListOrderByCreatedAtDesc(list);
    }

    @Transactional
    public void deleteComment(UUID commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));
        
        // Allowed if user is author of comment, creator of the list, or an admin
        if (!comment.getAuthor().getUserId().equals(user.getUserId()) &&
            !comment.getList().getCreator().getUserId().equals(user.getUserId()) &&
            !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Not authorized to delete comment");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public Notification triggerNotification(User recipient, String message, String type, String linkUrl) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLinkUrl(linkUrl);
        return notificationRepository.save(notification);
    }
}

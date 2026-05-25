package com.listitup.api.controller;

import com.listitup.api.model.Comment;
import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Controller
public class CommentController {

    private final CommentRepository commentRepository;
    private final CuratedListRepository listRepository;
    private final UserRepository userRepository;
    private final com.listitup.api.repository.NotificationRepository notificationRepository;

    public CommentController(CommentRepository commentRepository, CuratedListRepository listRepository, UserRepository userRepository, com.listitup.api.repository.NotificationRepository notificationRepository) {
        this.commentRepository = commentRepository;
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/lists/{id}/comments")
    @ResponseBody
    public ResponseEntity<?> addComment(@PathVariable UUID id, 
                             @RequestBody Map<String, String> payload,
                             @AuthenticationPrincipal OAuth2User oauthUser) {
        
        String text = payload.get("text");
        String email = oauthUser.getAttribute("email");
        User author = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listRepository.findById(id).orElseThrow();

        Comment comment = new Comment();
        comment.setText(text);
        comment.setAuthor(author);
        comment.setList(list);
        comment = commentRepository.save(comment);

        if (!author.getUserId().equals(list.getCreator().getUserId())) {
            com.listitup.api.model.Notification n = new com.listitup.api.model.Notification();
            n.setUser(list.getCreator());
            n.setMessage("💬 " + author.getUsername() + " commented on your list: " + list.getTitle());
            n.setLinkUrl("/lists/" + list.getListId());
            notificationRepository.save(n);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("commentId", comment.getCommentId());
        response.put("text", comment.getText());
        response.put("authorUsername", comment.getAuthor().getUsername());
        response.put("authorId", comment.getAuthor().getUserId());
        response.put("createdAt", comment.getCreatedAt().toString());
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/lists/{listId}/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable UUID listId, @PathVariable UUID commentId, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        
        if (!comment.getAuthor().getUserId().equals(user.getUserId()) && 
            !comment.getList().getCreator().getUserId().equals(user.getUserId()) &&
            !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        commentRepository.delete(comment);
        return ResponseEntity.ok(Map.of("success", true));
    }
}

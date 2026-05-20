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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class CommentController {

    private final CommentRepository commentRepository;
    private final CuratedListRepository listRepository;
    private final UserRepository userRepository;

    public CommentController(CommentRepository commentRepository, CuratedListRepository listRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.listRepository = listRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/lists/{id}/comments")
    public String addComment(@PathVariable UUID id, 
                             @RequestParam String text,
                             @AuthenticationPrincipal OAuth2User oauthUser) {
        
        String email = oauthUser.getAttribute("email");
        User author = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listRepository.findById(id).orElseThrow();

        Comment comment = new Comment();
        comment.setText(text);
        comment.setAuthor(author);
        comment.setList(list);
        commentRepository.save(comment);

        return "redirect:/lists/" + id;
    }
}

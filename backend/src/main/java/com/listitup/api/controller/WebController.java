package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CategoryRepository;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.CuratedListService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class WebController {

    private final CuratedListService listService;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public WebController(CuratedListService listService, CategoryRepository categoryRepository, CommentRepository commentRepository, UserRepository userRepository) {
        this.listService = listService;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/feed";
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(required = false) String category, Model model, jakarta.servlet.http.HttpServletRequest request) {
        List<CuratedList> lists;
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
            lists = listService.getListsByCategory(category);
            model.addAttribute("selectedCategory", category);
        } else {
            lists = listService.getAllLists();
            model.addAttribute("selectedCategory", "All");
        }
        model.addAttribute("lists", lists);
        model.addAttribute("categories", categoryRepository.findAll());
        
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            Object exception = session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            if (exception instanceof org.springframework.security.core.AuthenticationException) {
                String msg = ((org.springframework.security.core.AuthenticationException) exception).getMessage();
                if ("account_blocked".equals(msg)) {
                    model.addAttribute("errorMessage", "Your account has been blocked by an administrator.");
                } else {
                    model.addAttribute("errorMessage", "Authentication failed: " + msg);
                }
                session.removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            }
        }
        return "home"; // renders home.html
    }

    @GetMapping("/lists/{id}")
    public String listDetail(@PathVariable UUID id, Model model, @AuthenticationPrincipal OAuth2User oauthUser) {
        Optional<CuratedList> list = listService.getListById(id);
        if (list.isPresent()) {
            model.addAttribute("list", list.get());
            model.addAttribute("comments", commentRepository.findByListOrderByCreatedAtDesc(list.get()));
            
            // Add currentUser if logged in
            if (oauthUser != null) {
                String email = oauthUser.getAttribute("email");
                Optional<User> currentUser = userRepository.findByEmail(email);
                currentUser.ifPresent(user -> model.addAttribute("currentUser", user));
            }
            
            return "list-detail"; // renders list-detail.html
        }
        return "redirect:/feed";
    }

    @GetMapping("/lists/new")
    public String createList(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "create-edit-list"; // renders create-edit-list.html
    }
}
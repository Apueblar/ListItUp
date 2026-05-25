package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CategoryRepository;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.CuratedListRepository;
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
    private final CuratedListRepository listRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public WebController(CuratedListService listService, CuratedListRepository listRepository, CategoryRepository categoryRepository, CommentRepository commentRepository, UserRepository userRepository, jakarta.persistence.EntityManager entityManager) {
        this.listService = listService;
        this.listRepository = listRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/feed";
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(required = false) String category, 
                       @RequestParam(required = false, defaultValue = "recent") String sort,
                       Model model, jakarta.servlet.http.HttpServletRequest request) {
        
        User currentUser = null;
        if (request.getUserPrincipal() instanceof OAuth2User) {
            String email = ((OAuth2User) request.getUserPrincipal()).getAttribute("email");
            currentUser = userRepository.findByEmail(email).orElse(null);
        }

        if ("following".equalsIgnoreCase(sort) && currentUser == null) {
            return "redirect:/oauth2/authorization/google";
        }

        List<CuratedList> lists;
        boolean hasCategory = category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All");
        
        if ("trending".equalsIgnoreCase(sort)) {
            if (hasCategory) lists = listRepository.findByCategoryNameIgnoreCaseOrderByViewCountDesc(category);
            else lists = listRepository.findAllByOrderByViewCountDesc();
        } else if ("following".equalsIgnoreCase(sort) && currentUser != null) {
            if (hasCategory) lists = listRepository.findListsFromFollowedUsersByCategoryOrderByCreatedAtDesc(currentUser, category);
            else lists = listRepository.findListsFromFollowedUsersOrderByCreatedAtDesc(currentUser);
        } else {
            if (hasCategory) lists = listRepository.findByCategoryNameIgnoreCaseOrderByCreatedAtDesc(category);
            else lists = listRepository.findAllByOrderByCreatedAtDesc();
        }
        // Exclude drafts from public feed
        lists = lists.stream().filter(l -> !Boolean.TRUE.equals(l.getIsDraft())).collect(java.util.stream.Collectors.toList());

        model.addAttribute("selectedCategory", hasCategory ? category : "All");
        model.addAttribute("selectedSort", sort.toLowerCase());
        model.addAttribute("lists", lists);
        model.addAttribute("categories", categoryRepository.findAll());
        
        if (request.getUserPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) request.getUserPrincipal();
            String email = oauthUser.getAttribute("email");
            Optional<User> currentUserOpt = userRepository.findByEmail(email);
            currentUserOpt.ifPresent(user -> {
                model.addAttribute("currentUser", user);
                List<UUID> likedListIds = entityManager.createQuery("SELECT l.list.listId FROM Like l WHERE l.user = :u", UUID.class)
                        .setParameter("u", user)
                        .getResultList();
                model.addAttribute("likedListIds", likedListIds);
            });
        }

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
    public String listDetail(@PathVariable UUID id, Model model, @AuthenticationPrincipal OAuth2User oauthUser,
                             jakarta.servlet.http.HttpServletRequest request) {
        Optional<CuratedList> list = listService.getListById(id);
        if (list.isPresent()) {
            CuratedList curatedList = list.get();

            // Increment view count (once per session per list)
            jakarta.servlet.http.HttpSession session = request.getSession(true);
            String viewedKey = "viewed_" + id;
            if (session.getAttribute(viewedKey) == null) {
                curatedList.setViewCount(curatedList.getViewCount() + 1);
                listService.saveList(curatedList);
                session.setAttribute(viewedKey, true);
            }

            model.addAttribute("list", curatedList);
            model.addAttribute("comments", commentRepository.findByListOrderByCreatedAtDesc(curatedList));
            
            long totalLikes = (long) entityManager.createQuery("SELECT COUNT(l) FROM Like l WHERE l.list = :list")
                    .setParameter("list", curatedList)
                    .getSingleResult();
            model.addAttribute("totalLikes", totalLikes);

            // Add currentUser if logged in
            if (oauthUser != null) {
                String email = oauthUser.getAttribute("email");
                Optional<User> currentUser = userRepository.findByEmail(email);
                currentUser.ifPresent(user -> {
                    model.addAttribute("currentUser", user);
                    
                    long likeCount = (long) entityManager.createQuery("SELECT COUNT(l) FROM Like l WHERE l.user = :u AND l.list = :list")
                            .setParameter("u", user)
                            .setParameter("list", curatedList)
                            .getSingleResult();
                    model.addAttribute("isLiked", likeCount > 0);

                    long saveCount = (long) entityManager.createQuery("SELECT COUNT(s) FROM SavedList s WHERE s.user = :u AND s.list = :list")
                            .setParameter("u", user)
                            .setParameter("list", curatedList)
                            .getSingleResult();
                    model.addAttribute("isSaved", saveCount > 0);
                });
            }
            
            return "list-detail";
        }
        return "redirect:/feed";
    }

    @GetMapping("/lists/new")
    public String createList(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "create-edit-list";
    }

    @GetMapping("/lists/{id}/edit")
    public String editList(@PathVariable UUID id, Model model, @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) {
            return "redirect:/oauth2/authorization/google";
        }
        String email = oauthUser.getAttribute("email");
        Optional<User> currentUser = userRepository.findByEmail(email);
        Optional<CuratedList> listOpt = listService.getListById(id);

        if (currentUser.isPresent() && listOpt.isPresent()) {
            CuratedList list = listOpt.get();
            if (list.getCreator().getUserId().equals(currentUser.get().getUserId())) {
                model.addAttribute("list", list);
                model.addAttribute("categories", categoryRepository.findAll());
                return "create-edit-list";
            }
        }
        return "redirect:/lists/" + id;
    }

    @GetMapping("/profile/drafts")
    public String myDrafts(Model model, @AuthenticationPrincipal OAuth2User oauthUser) {
        if (oauthUser == null) {
            return "redirect:/oauth2/authorization/google";
        }
        String email = oauthUser.getAttribute("email");
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        List<CuratedList> drafts = listRepository.findByCreatorAndIsDraftTrueOrderByCreatedAtDesc(currentUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("drafts", drafts);
        return "drafts";
    }
}
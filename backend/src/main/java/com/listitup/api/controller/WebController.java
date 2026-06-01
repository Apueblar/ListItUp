package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CategoryRepository;
import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.CuratedListService;
import com.listitup.api.service.TrendingService;
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
    private final TrendingService trendingService;

    public WebController(CuratedListService listService, CuratedListRepository listRepository, CategoryRepository categoryRepository, CommentRepository commentRepository, UserRepository userRepository, jakarta.persistence.EntityManager entityManager, TrendingService trendingService) {
        this.listService = listService;
        this.listRepository = listRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
        this.trendingService = trendingService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/feed";
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(required = false) String category, 
                       @RequestParam(required = false, defaultValue = "recent") String sort,
                       Model model, jakarta.servlet.http.HttpServletRequest request) {
        
        // Extract the logged-in user from the security context
        User currentUser = null;
        OAuth2User oauthPrincipal = null;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OAuth2User) {
            oauthPrincipal = (OAuth2User) auth.getPrincipal();
            String email = oauthPrincipal.getAttribute("email");
            currentUser = userRepository.findByEmail(email).orElse(null);
        }

        if ("following".equalsIgnoreCase(sort) && currentUser == null) {
            return "redirect:/oauth2/authorization/google";
        }

        List<CuratedList> lists;
        boolean hasCategory = category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All");
        
        if ("trending".equalsIgnoreCase(sort)) {
            if (hasCategory) lists = listRepository.findByCategoryNameIgnoreCaseOrderByCreatedAtDesc(category);
            else lists = listRepository.findAllByOrderByCreatedAtDesc();
            
            // Sort by 24h views, then lifetime views, then creation date
            Map<UUID, Integer> recentViews = trendingService.getRecentViewCounts();
            lists.sort((l1, l2) -> {
                int v1 = recentViews.getOrDefault(l1.getListId(), 0);
                int v2 = recentViews.getOrDefault(l2.getListId(), 0);
                if (v1 != v2) {
                    return Integer.compare(v2, v1);
                }
                int tv1 = l1.getViewCount() != null ? l1.getViewCount() : 0;
                int tv2 = l2.getViewCount() != null ? l2.getViewCount() : 0;
                if (tv1 != tv2) {
                    return Integer.compare(tv2, tv1);
                }
                return l2.getCreatedAt().compareTo(l1.getCreatedAt());
            });
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
        
        if (currentUser != null) {
            final User finalUser = currentUser;
            model.addAttribute("currentUser", finalUser);
            List<UUID> likedListIds = entityManager.createQuery("SELECT l.list.listId FROM Like l WHERE l.user = :u", UUID.class)
                    .setParameter("u", finalUser)
                    .getResultList();
            model.addAttribute("likedListIds", likedListIds);
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
                trendingService.recordView(id);
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
package com.listitup.api.controller;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.CuratedListService;
import com.listitup.api.repository.CategoryRepository;
import com.listitup.api.model.Category;
import com.listitup.api.repository.CategoryProposalRepository;
import com.listitup.api.model.CategoryProposal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.listitup.api.repository.CommentRepository;
import com.listitup.api.repository.ItemRepository;
import com.listitup.api.repository.ReportRepository;
import com.listitup.api.model.Report;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final CuratedListService listService;
    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryProposalRepository categoryProposalRepository;
    private final CommentRepository commentRepository;
    private final ItemRepository itemRepository;

    public AdminController(UserRepository userRepository, CuratedListService listService, ReportRepository reportRepository, CategoryRepository categoryRepository, CategoryProposalRepository categoryProposalRepository, CommentRepository commentRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.listService = listService;
        this.reportRepository = reportRepository;
        this.categoryRepository = categoryRepository;
        this.categoryProposalRepository = categoryProposalRepository;
        this.commentRepository = commentRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping("/admin")
    public String adminPanel(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin"; // renders admin.html
    }

    @PostMapping("/admin/users/role")
    public String updateUserRole(@RequestParam UUID userId, @RequestParam String role) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Restrict modifying the main admin's role to prevent lockouts
            if (!user.getEmail().equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
                user.setRole(role);
                syncPrivileges(user);
                userRepository.save(user);
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/toggle-block")
    public String toggleUserBlock(@RequestParam UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Restrict blocking the main admin
            if (!user.getEmail().equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
                user.setIsBlocked(!Boolean.TRUE.equals(user.getIsBlocked()));
                userRepository.save(user);
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/toggle-badge")
    public String toggleUserBadge(@RequestParam UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setHasBadge(!Boolean.TRUE.equals(user.getHasBadge()));
            // If they get verified, let's promote them to VERIFIED role as well if they are STANDARD
            if (Boolean.TRUE.equals(user.getHasBadge()) && "STANDARD".equals(user.getRole())) {
                user.setRole("VERIFIED");
            } else if (!Boolean.TRUE.equals(user.getHasBadge()) && "VERIFIED".equals(user.getRole())) {
                user.setRole("STANDARD");
            }
            syncPrivileges(user);
            userRepository.save(user);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/users/{userId}/delete")
    public String deleteUserAccount(@PathVariable UUID userId) {
        listService.deleteUser(userId);
        return "redirect:/admin";
    }

    private void syncPrivileges(User user) {
        String role = user.getRole();
        if ("VERIFIED".equals(role)) {
            user.setHasBadge(true);
            user.setCanPinLists(true);
            user.setHasAnalyticsAccess(true);
            user.setCanModerateContent(false);
            user.setCanDeleteAny(false);
        } else if ("ADMIN".equals(role)) {
            user.setHasBadge(true);
            user.setCanPinLists(true);
            user.setHasAnalyticsAccess(true);
            user.setCanModerateContent(true);
            user.setCanDeleteAny(true);
        } else { // STANDARD
            user.setCanPinLists(false);
            user.setHasAnalyticsAccess(false);
            user.setCanModerateContent(false);
            user.setCanDeleteAny(false);
        }
    }

    @GetMapping("/admin/lists")
    public String adminListsPanel(Model model) {
        model.addAttribute("lists", listService.getAllPublicLists());
        return "admin-lists";
    }

    @PostMapping("/admin/lists/{id}/delete")
    public String adminDeleteList(@PathVariable UUID id) {
        listService.deleteList(id);
        return "redirect:/admin/lists";
    }

    @GetMapping("/admin/reports")
    public String viewReports(Model model) {
        List<Report> pendingReports = reportRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        List<Report> resolvedReports = reportRepository.findByStatusOrderByCreatedAtDesc("RESOLVED");
        List<Report> dismissedReports = reportRepository.findByStatusOrderByCreatedAtDesc("DISMISSED");

        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("resolvedReports", resolvedReports);
        model.addAttribute("dismissedReports", dismissedReports);

        return "admin-reports";
    }

    @PostMapping("/admin/reports/{id}/resolve")
    public String resolveReport(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        User admin = null;
        if (oauthUser != null) {
            String email = oauthUser.getAttribute("email");
            admin = userRepository.findByEmail(email).orElse(null);
        }
        final User finalAdmin = admin;
        reportRepository.findById(id).ifPresent(report -> {
            
            // 1. Resolve all reports for this same content, and nullify the target so we can delete it
            if (report.getTargetList() != null) {
                CuratedList target = report.getTargetList();
                List<Report> linkedReports = reportRepository.findByTargetList(target);
                for(Report r : linkedReports) {
                    r.setStatus("RESOLVED");
                    if (finalAdmin != null) r.setReviewedByAdmin(finalAdmin);
                    r.setTargetList(null);
                    reportRepository.save(r);
                }
                listService.deleteList(target.getListId());
            } else if (report.getTargetItem() != null) {
                Item target = report.getTargetItem();
                List<Report> linkedReports = reportRepository.findByTargetItem(target);
                for(Report r : linkedReports) {
                    r.setStatus("RESOLVED");
                    if (finalAdmin != null) r.setReviewedByAdmin(finalAdmin);
                    r.setTargetItem(null);
                    reportRepository.save(r);
                }
                itemRepository.delete(target);
            } else if (report.getTargetComment() != null) {
                Comment target = report.getTargetComment();
                List<Report> linkedReports = reportRepository.findByTargetComment(target);
                for(Report r : linkedReports) {
                    r.setStatus("RESOLVED");
                    if (finalAdmin != null) r.setReviewedByAdmin(finalAdmin);
                    r.setTargetComment(null);
                    reportRepository.save(r);
                }
                commentRepository.delete(target);
            } else {
                // Failsafe if target is already null
                report.setStatus("RESOLVED");
                if (finalAdmin != null) report.setReviewedByAdmin(finalAdmin);
                reportRepository.save(report);
            }
        });
        return "redirect:/admin/reports";
    }

    @PostMapping("/admin/reports/{id}/dismiss")
    public String dismissReport(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        User admin = null;
        if (oauthUser != null) {
            String email = oauthUser.getAttribute("email");
            admin = userRepository.findByEmail(email).orElse(null);
        }
        final User finalAdmin = admin;
        reportRepository.findById(id).ifPresent(report -> {
            report.setStatus("DISMISSED");
            if (finalAdmin != null) {
                report.setReviewedByAdmin(finalAdmin);
            }
            reportRepository.save(report);
        });
        return "redirect:/admin/reports";
    }

    @PostMapping("/admin/reports/{reportId}/delete-comment/{commentId}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteReportedComment(@PathVariable UUID reportId, @PathVariable UUID commentId, @AuthenticationPrincipal OAuth2User oauthUser) {
        User admin = null;
        if (oauthUser != null) {
            String email = oauthUser.getAttribute("email");
            admin = userRepository.findByEmail(email).orElse(null);
        }
        final User finalAdmin = admin;
        commentRepository.findById(commentId).ifPresent(commentRepository::delete);
        reportRepository.findById(reportId).ifPresent(report -> {
            report.setStatus("RESOLVED");
            if (finalAdmin != null) {
                report.setReviewedByAdmin(finalAdmin);
            }
            reportRepository.save(report);
        });
        return "redirect:/admin/reports";
    }

    @GetMapping("/admin/categories")
    public String adminCategoriesPanel(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("pendingProposals", categoryProposalRepository.findByStatusOrderByCreatedAtDesc("PENDING"));
        return "admin-categories";
    }

    @PostMapping("/admin/categories")
    public String createCategory(@RequestParam String name, @RequestParam(required = false) String icon) {
        Category category = new Category();
        category.setName(name);
        category.setIcon(icon);
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/edit")
    public String editCategory(@PathVariable UUID id, @RequestParam String name, @RequestParam(required = false) String icon) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            category.setName(name);
            category.setIcon(icon);
            categoryRepository.save(category);
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String deleteCategory(@PathVariable UUID id, 
                                 @RequestParam(required = false) UUID replacementCategoryId,
                                 RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isPresent()) {
            Category categoryToDelete = categoryOpt.get();
            boolean hasLists = !listService.getListsByCategory(categoryToDelete.getName()).isEmpty();
            
            if (hasLists) {
                if (replacementCategoryId == null || replacementCategoryId.equals(id)) {
                    redirectAttributes.addFlashAttribute("error", "Cannot delete category '" + categoryToDelete.getName() + "' because it has associated lists and no replacement category was selected.");
                    return "redirect:/admin/categories";
                }
                
                Optional<Category> replacementOpt = categoryRepository.findById(replacementCategoryId);
                if (replacementOpt.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Replacement category not found.");
                    return "redirect:/admin/categories";
                }
                
                Category replacementCategory = replacementOpt.get();
                listService.updateCategoryForAll(categoryToDelete, replacementCategory);
            }
            
            try {
                categoryRepository.delete(categoryToDelete);
                redirectAttributes.addFlashAttribute("success", "Category '" + categoryToDelete.getName() + "' deleted successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Could not delete category: " + e.getMessage());
            }
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/proposals/{id}/approve")
    public String approveProposal(@PathVariable UUID id, @RequestParam(required = false) String icon) {
        categoryProposalRepository.findById(id).ifPresent(proposal -> {
            proposal.setStatus("APPROVED");
            categoryProposalRepository.save(proposal);

            Category category = new Category();
            category.setName(proposal.getProposedName());
            category.setIcon(icon);
            categoryRepository.save(category);
        });
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/proposals/{id}/reject")
    public String rejectProposal(@PathVariable UUID id) {
        categoryProposalRepository.findById(id).ifPresent(proposal -> {
            proposal.setStatus("REJECTED");
            categoryProposalRepository.save(proposal);
        });
        return "redirect:/admin/categories";
    }
}

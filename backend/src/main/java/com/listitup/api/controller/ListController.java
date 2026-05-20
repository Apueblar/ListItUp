package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Category;
import com.listitup.api.model.User;
import com.listitup.api.repository.CategoryRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.CuratedListService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ListController {

    private final CuratedListService listService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ListController(CuratedListService listService, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.listService = listService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/lists")
    public String createList(@ModelAttribute com.listitup.api.dto.ListCreationDTO dto, 
                             @AuthenticationPrincipal OAuth2User oauthUser) {
        
        String email = oauthUser.getAttribute("email");
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        CuratedList newList = new CuratedList();
        newList.setTitle(dto.getTitle());
        newList.setDescription(dto.getDescription());
        newList.setCategory(category);
        newList.setCreator(creator);
        
        java.util.List<com.listitup.api.model.Item> items = new java.util.ArrayList<>();
        int index = 1;
        if (dto.getItems() != null) {
            for (com.listitup.api.dto.ItemCreationDTO itemDto : dto.getItems()) {
                if (itemDto.getTitle() != null && !itemDto.getTitle().trim().isEmpty()) {
                    com.listitup.api.model.Item item = new com.listitup.api.model.Item();
                    item.setTitle(itemDto.getTitle());
                    item.setDescription(itemDto.getDescription());
                    item.setExternalUrl(itemDto.getExternalUrl());
                    item.setPositionIndex(index++);
                    item.setList(newList);
                    items.add(item);
                }
            }
        }
        newList.setItems(items);
        
        CuratedList savedList = listService.saveList(newList);
        return "redirect:/lists/" + savedList.getListId();
    }
}

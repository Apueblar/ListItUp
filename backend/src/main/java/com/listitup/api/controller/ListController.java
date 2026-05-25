package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Category;
import com.listitup.api.model.Item;
import com.listitup.api.model.User;
import com.listitup.api.dto.ItemCreationDTO;
import com.listitup.api.dto.ListCreationDTO;
import com.listitup.api.repository.CategoryRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.service.CuratedListService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    private List<Item> buildItems(List<ItemCreationDTO> dtoItems, CuratedList list) {
        List<Item> items = new ArrayList<>();
        int index = 1;
        if (dtoItems != null) {
            for (ItemCreationDTO itemDto : dtoItems) {
                if (itemDto.getTitle() != null && !itemDto.getTitle().trim().isEmpty()) {
                    Item item = new Item();
                    item.setTitle(itemDto.getTitle());
                    item.setDescription(itemDto.getDescription());
                    item.setExternalUrl(itemDto.getExternalUrl());
                    item.setPhoto(itemDto.getPhoto());
                    item.setPositionIndex(index++);
                    item.setList(list);
                    items.add(item);
                }
            }
        }
        return items;
    }

    @PostMapping("/lists")
    public String createList(@ModelAttribute ListCreationDTO dto,
                             @RequestParam(required = false) String action,
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
        newList.setCoverPhoto(dto.getCoverPhoto());
        newList.setIsDraft("draft".equals(action));
        newList.setItems(buildItems(dto.getItems(), newList));

        CuratedList savedList = listService.saveList(newList);

        if (Boolean.TRUE.equals(savedList.getIsDraft())) {
            return "redirect:/profile/drafts";
        }
        return "redirect:/lists/" + savedList.getListId();
    }

    @PostMapping("/lists/{id}/edit")
    public String editList(@PathVariable UUID id, @ModelAttribute ListCreationDTO dto,
                           @RequestParam(required = false) String action,
                           @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listService.getListById(id).orElseThrow();

        if (!list.getCreator().getUserId().equals(currentUser.getUserId())) {
            return "redirect:/lists/" + id;
        }

        Category category = categoryRepository.findById(dto.getCategoryId()).orElseThrow();

        list.setTitle(dto.getTitle());
        list.setDescription(dto.getDescription());
        list.setCategory(category);
        list.setCoverPhoto(dto.getCoverPhoto());
        list.setIsDraft("draft".equals(action));

        list.getItems().clear();
        list.getItems().addAll(buildItems(dto.getItems(), list));

        listService.saveList(list);

        if (Boolean.TRUE.equals(list.getIsDraft())) {
            return "redirect:/profile/drafts";
        }
        return "redirect:/lists/" + list.getListId();
    }

    @PostMapping("/lists/{id}/publish")
    public String publishDraft(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listService.getListById(id).orElseThrow();

        if (!list.getCreator().getUserId().equals(currentUser.getUserId())) {
            return "redirect:/profile/drafts";
        }

        list.setIsDraft(false);
        listService.saveList(list);
        return "redirect:/lists/" + list.getListId();
    }

    @PostMapping("/lists/{id}/delete")
    public String deleteList(@PathVariable UUID id, @AuthenticationPrincipal OAuth2User oauthUser) {
        String email = oauthUser.getAttribute("email");
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        CuratedList list = listService.getListById(id).orElseThrow();

        if (!list.getCreator().getUserId().equals(currentUser.getUserId())) {
            return "redirect:/lists/" + id;
        }

        listService.deleteList(id);
        return "redirect:/feed";
    }
}

package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.repository.CuratedListRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    private final CuratedListRepository listRepository;
    private final com.listitup.api.repository.CategoryRepository categoryRepository;

    public SearchController(CuratedListRepository listRepository, com.listitup.api.repository.CategoryRepository categoryRepository) {
        this.listRepository = listRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false, defaultValue = "") String q,
                         @RequestParam(required = false, defaultValue = "All") String category,
                         @RequestParam(required = false, defaultValue = "relevance") String sort,
                         @RequestParam(required = false, defaultValue = "0") int page,
                         Model model) {
        
        org.springframework.data.domain.Pageable pageable;
        if ("recency".equalsIgnoreCase(sort)) {
            pageable = org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by("createdAt").descending());
        } else {
            pageable = org.springframework.data.domain.PageRequest.of(page, 10, org.springframework.data.domain.Sort.by("viewCount").descending());
        }

        org.springframework.data.domain.Page<CuratedList> listsPage;
        if (q == null || q.trim().isEmpty()) {
            if ("All".equalsIgnoreCase(category)) {
                listsPage = listRepository.findAllPublicSearchable(pageable);
            } else {
                listsPage = listRepository.findByCategoryPublicSearchable(category, pageable);
            }
        } else {
            if ("All".equalsIgnoreCase(category)) {
                listsPage = listRepository.searchLists(q, pageable);
            } else {
                listsPage = listRepository.searchListsByCategory(q, category, pageable);
            }
        }

        model.addAttribute("query", q);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("listsPage", listsPage);
        model.addAttribute("categories", categoryRepository.findAll());

        return "search";
    }
}

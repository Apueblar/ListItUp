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

    public SearchController(CuratedListRepository listRepository) {
        this.listRepository = listRepository;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "q", required = false) String query, Model model) {
        List<CuratedList> results;
        if (query == null || query.isBlank()) {
            results = List.of();
        } else {
            results = listRepository.findByTitleContainingIgnoreCase(query);
        }
        model.addAttribute("results", results);
        model.addAttribute("query", query);
        return "search"; // Thymeleaf template search.html
    }
}

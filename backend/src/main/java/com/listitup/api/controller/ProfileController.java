package com.listitup.api.controller;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.SavedList;
import com.listitup.api.model.User;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.SavedListRepository;
import com.listitup.api.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final CuratedListRepository listRepository;
    private final SavedListRepository savedListRepository;

    public ProfileController(UserRepository userRepository, CuratedListRepository listRepository, SavedListRepository savedListRepository) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.savedListRepository = savedListRepository;
    }

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal OAuth2User oauthUser, Model model) {
        String email = oauthUser.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();

        List<CuratedList> myLists = listRepository.findByCreatorOrderByCreatedAtDesc(user);
        
        List<CuratedList> savedLists = savedListRepository.findByUserOrderBySavedAtDesc(user)
                .stream()
                .map(SavedList::getList)
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("myLists", myLists);
        model.addAttribute("savedLists", savedLists);

        return "profile";
    }
}

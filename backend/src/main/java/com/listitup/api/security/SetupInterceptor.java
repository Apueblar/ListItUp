package com.listitup.api.security;

import com.listitup.api.model.User;
import com.listitup.api.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SetupInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public SetupInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Exclude static assets, setup page itself, error, and logout endpoints from redirecting
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") 
            || uri.equals("/setup-username") || uri.equals("/logout") || uri.equals("/error")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
            String email = oauthUser.getAttribute("email");
            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && !Boolean.TRUE.equals(user.getHasCompletedSetup())) {
                    response.sendRedirect("/setup-username");
                    return false;
                }
            }
        }

        return true;
    }
}

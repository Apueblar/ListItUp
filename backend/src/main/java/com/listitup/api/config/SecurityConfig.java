package com.listitup.api.config;

import com.listitup.api.security.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(CustomOidcUserService customOidcUserService) {
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Specific list creation/edit endpoints
                .requestMatchers("/lists/new").hasAnyRole("ADMIN", "VERIFIED")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists").hasAnyRole("ADMIN", "VERIFIED")
                // Public endpoints
                .requestMatchers("/", "/feed", "/search", "/categories", "/lists/**", "/css/**", "/js/**", "/images/**", "/error", "/logout").permitAll()
                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Analytics endpoints
                .requestMatchers("/analytics/**").hasAnyRole("VERIFIED", "ADMIN")
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                .defaultSuccessUrl("/feed", true)
                .failureUrl("/feed?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/perform_logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // Use cookie-based CSRF so our JS can read the token
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );

        return http.build();
    }
}


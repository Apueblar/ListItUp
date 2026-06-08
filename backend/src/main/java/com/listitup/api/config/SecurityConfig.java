package com.listitup.api.config;

import com.listitup.api.security.CustomOidcUserService;
import com.listitup.api.security.CustomOAuth2UserService;
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
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOidcUserService customOidcUserService, CustomOAuth2UserService customOAuth2UserService) {
        this.customOidcUserService = customOidcUserService;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Specific list creation/edit endpoints
                .requestMatchers("/lists/new").authenticated()
                .requestMatchers("/lists/*/edit").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists/*/delete").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/*/follow").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/users/*/follow").authenticated()
                // List interaction endpoints require authentication (must be before the broad /lists/** permitAll)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists/*/like").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists/*/save").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists/*/pin").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/lists/*/comments").authenticated()
                // Public endpoints
                .requestMatchers("/", "/feed", "/search", "/categories", "/lists/**", "/users/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/error", "/logout", "/actuator/health", "/og").permitAll()
                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Analytics endpoints
                .requestMatchers("/analytics/**").hasRole("VERIFIED")
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOidcUserService)
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/feed", true)
                .failureUrl("/feed?error=true")
            )
            .logout(logout -> logout
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
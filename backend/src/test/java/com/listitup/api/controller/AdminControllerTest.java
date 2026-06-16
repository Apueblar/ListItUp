package com.listitup.api.controller;

import com.listitup.api.model.*;
import com.listitup.api.repository.*;
import com.listitup.api.security.CustomOAuth2UserService;
import com.listitup.api.security.CustomOidcUserService;
import com.listitup.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import org.springframework.context.annotation.Import;
import com.listitup.api.config.SecurityConfig;
import com.listitup.api.security.SetupInterceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import({SecurityConfig.class, SetupInterceptor.class})
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Security dependencies
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private CustomOidcUserService customOidcUserService;

    // Repository / service mocks
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CuratedListService listService;
    @MockBean
    private ReportRepository reportRepository;
    @MockBean
    private CategoryRepository categoryRepository;
    @MockBean
    private CategoryProposalRepository categoryProposalRepository;
    @MockBean
    private CommentRepository commentRepository;
    @MockBean
    private ItemRepository itemRepository;
    @MockBean
    private SessionRegistry sessionRegistry;
    @MockBean
    private NotificationRepository notificationRepository;
    @MockBean
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;
    @MockBean
    private SavedListRepository savedListRepository;
    @MockBean
    private ListAnalyticsRepository listAnalyticsRepository;

    private User adminUser;
    private User standardUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(UUID.randomUUID());
        adminUser.setEmail("admin@example.com");
        adminUser.setUsername("admin");
        adminUser.setRole("ADMIN");
        adminUser.setAuthProvider("GOOGLE");
        adminUser.setHasCompletedSetup(true);

        standardUser = new User();
        standardUser.setUserId(UUID.randomUUID());
        standardUser.setEmail("standard@example.com");
        standardUser.setUsername("standard");
        standardUser.setRole("STANDARD");
        standardUser.setAuthProvider("GOOGLE");
        standardUser.setHasCompletedSetup(true);
    }

    @Test
    void testAdminPanel_RedirectsToFeedForGuest() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testAdminPanel_RedirectsToFeedForStandardUser() throws Exception {
        when(userRepository.findFirstByEmail("standard@example.com")).thenReturn(Optional.of(standardUser));

        mockMvc.perform(get("/admin")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STANDARD"))
                                .attributes(attrs -> attrs.put("email", "standard@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminPanel_ReturnsOkForAdminUser() throws Exception {
        when(userRepository.findFirstByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(List.of(adminUser, standardUser));

        mockMvc.perform(get("/admin")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                .attributes(attrs -> attrs.put("email", "admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void testAdminListsPanel_RedirectsToFeedForStandardUser() throws Exception {
        when(userRepository.findFirstByEmail("standard@example.com")).thenReturn(Optional.of(standardUser));

        mockMvc.perform(get("/admin/lists")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STANDARD"))
                                .attributes(attrs -> attrs.put("email", "standard@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminListsPanel_ReturnsOkForAdminUser() throws Exception {
        when(userRepository.findFirstByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(listService.getAllPublicLists()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/lists")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                .attributes(attrs -> attrs.put("email", "admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-lists"));
    }

    @Test
    void testViewReports_RedirectsToFeedForStandardUser() throws Exception {
        when(userRepository.findFirstByEmail("standard@example.com")).thenReturn(Optional.of(standardUser));

        mockMvc.perform(get("/admin/reports")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STANDARD"))
                                .attributes(attrs -> attrs.put("email", "standard@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testViewReports_ReturnsOkForAdminUser() throws Exception {
        when(userRepository.findFirstByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(reportRepository.findByStatusOrderByCreatedAtDesc(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/reports")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                .attributes(attrs -> attrs.put("email", "admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-reports"));
    }

    @Test
    void testUpdateUserRole_SuccessForAdminUser() throws Exception {
        when(userRepository.findById(standardUser.getUserId())).thenReturn(Optional.of(standardUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/admin/users/role")
                        .with(csrf())
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                .attributes(attrs -> attrs.put("email", "admin@example.com")))
                        .param("userId", standardUser.getUserId().toString())
                        .param("role", "VERIFIED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }
}

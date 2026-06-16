package com.listitup.api.security;

import com.listitup.api.model.User;
import com.listitup.api.repository.*;
import com.listitup.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CuratedListRepository listRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private CommentRepository commentRepository;

    @MockBean
    private CuratedListService listService;

    @MockBean
    private AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @MockBean
    private ReportRepository reportRepository;

    @MockBean
    private CategoryProposalRepository categoryProposalRepository;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private SavedListRepository savedListRepository;

    @MockBean
    private ListAnalyticsRepository listAnalyticsRepository;

    @MockBean
    private ItemRepository itemRepository;

    private User adminUser;
    private User standardUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setUsername("admin");
        adminUser.setRole("ADMIN");
        adminUser.setAuthProvider("GOOGLE");
        adminUser.setHasCompletedSetup(true);

        standardUser = new User();
        standardUser.setEmail("standard@example.com");
        standardUser.setUsername("standard");
        standardUser.setRole("STANDARD");
        standardUser.setAuthProvider("GOOGLE");
        standardUser.setHasCompletedSetup(true);
    }

    @Test
    void testPublicFeed_PermitAll() throws Exception {
        mockMvc.perform(get("/feed"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminRoute_RequiresAuth() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testAdminRoute_ForbiddenForStandardUser() throws Exception {
        when(userRepository.findFirstByEmail("standard@example.com")).thenReturn(Optional.of(standardUser));
        
        mockMvc.perform(get("/admin")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STANDARD"))
                                .attributes(attrs -> attrs.put("email", "standard@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminRoute_AllowedForAdminUser() throws Exception {
        when(userRepository.findFirstByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin")
                        .with(oauth2Login()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                .attributes(attrs -> attrs.put("email", "admin@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateListRoute_RequiresAuth() throws Exception {
        mockMvc.perform(get("/lists/new"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testPostListWithoutCsrf_Forbidden() throws Exception {
        mockMvc.perform(post("/lists")
                        .param("title", "Forbidden List"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOAuth2LoginPage_Available() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }
}

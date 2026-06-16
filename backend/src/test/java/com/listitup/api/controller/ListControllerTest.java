package com.listitup.api.controller;

import com.listitup.api.model.*;
import com.listitup.api.repository.*;
import com.listitup.api.security.CustomOAuth2UserService;
import com.listitup.api.security.CustomOidcUserService;
import com.listitup.api.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.context.annotation.Import;
import com.listitup.api.config.SecurityConfig;
import com.listitup.api.security.SetupInterceptor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ListController.class, WebController.class})
@Import({SecurityConfig.class, SetupInterceptor.class})
public class ListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /* --- Security layer mocks ------------------------------------------- */
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private CustomOidcUserService   customOidcUserService;

    /* --- Repository / service mocks -------------------------------------- */
    @MockBean private CuratedListService       listService;
    @MockBean private CuratedListRepository    listRepository;
    @MockBean private CategoryRepository       categoryRepository;
    @MockBean private CommentRepository        commentRepository;
    @MockBean private UserRepository           userRepository;
    @MockBean private EntityManager            entityManager;
    @MockBean private TrendingService          trendingService;
    @MockBean private NotificationRepository   notificationRepository;
    @MockBean private AnalyticsSnapshotRepository analyticsSnapshotRepository;
    @MockBean private ReportRepository         reportRepository;
    @MockBean private CategoryProposalRepository categoryProposalRepository;
    @MockBean private ItemRepository           itemRepository;
    @MockBean private SavedListRepository      savedListRepository;
    @MockBean private ListAnalyticsRepository  listAnalyticsRepository;

    private User        testUser;
    private Category    testCategory;
    private CuratedList testList;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setEmail("user@example.com");
        testUser.setUsername("user");
        testUser.setRole("STANDARD");
        testUser.setAuthProvider("GOOGLE");
        testUser.setHasCompletedSetup(true);

        testCategory = new Category();
        testCategory.setCategoryId(UUID.randomUUID());
        testCategory.setName("Tech");

        testList = new CuratedList();
        testList.setListId(UUID.randomUUID());
        testList.setTitle("Best Stuff");
        testList.setCreator(testUser);
        testList.setCategory(testCategory);
        testList.setIsDraft(false);
        testList.setViewCount(0);
        testList.setItems(new ArrayList<>());   // prevent NPE in editList
    }

    /* =====================================================================
     * Feed — public endpoint
     * ===================================================================== */

    @Test
    void testGetFeed_Unauthenticated_ReturnsOk() throws Exception {
        when(listRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/feed"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    void testGetFeed_Authenticated_ReturnsOk() throws Exception {
        when(userRepository.findFirstByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(listRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        jakarta.persistence.TypedQuery<UUID> mockQuery = mock(jakarta.persistence.TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(UUID.class))).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/feed")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    /* =====================================================================
     * List detail — public endpoint
     * ===================================================================== */

    @Test
    void testGetListDetail_PublicList_ReturnsOk() throws Exception {
        when(listService.getListById(testList.getListId())).thenReturn(Optional.of(testList));
        when(commentRepository.findByListOrderByCreatedAtDesc(testList))
                .thenReturn(Collections.emptyList());
        when(listService.saveList(any())).thenReturn(testList);

        // The controller uses untyped createQuery(String) — getSingleResult() must return Long not null
        jakarta.persistence.Query mockQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(0L);

        mockMvc.perform(get("/lists/" + testList.getListId()))
                .andExpect(status().isOk())
                .andExpect(view().name("list-detail"));
    }

    @Test
    void testGetListDetail_NotFound_RedirectsToFeed() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(listService.getListById(unknownId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/lists/" + unknownId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feed"));
    }

    /* =====================================================================
     * Create list — requires auth
     * ===================================================================== */

    @Test
    void testCreateList_Unauthenticated_Redirects() throws Exception {
        mockMvc.perform(post("/lists")
                        .with(csrf())
                        .param("title", "New List")
                        .param("categoryId", UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testNewListForm_Unauthenticated_Redirects() throws Exception {
        mockMvc.perform(get("/lists/new"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testCreateList_AuthenticatedSuccess() throws Exception {
        when(userRepository.findFirstByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getCategoryId())).thenReturn(Optional.of(testCategory));
        when(listService.saveList(any())).thenReturn(testList);

        mockMvc.perform(post("/lists")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "user@example.com")))
                        .param("title", "Best Stuff")
                        .param("categoryId", testCategory.getCategoryId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/" + testList.getListId()));
    }

    @Test
    void testCreateList_DraftSuccess() throws Exception {
        CuratedList draft = new CuratedList();
        draft.setListId(UUID.randomUUID());
        draft.setIsDraft(true);
        draft.setItems(new ArrayList<>());

        when(userRepository.findFirstByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(testCategory.getCategoryId())).thenReturn(Optional.of(testCategory));
        when(listService.saveList(any())).thenReturn(draft);

        mockMvc.perform(post("/lists")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "user@example.com")))
                        .param("title", "Draft")
                        .param("categoryId", testCategory.getCategoryId().toString())
                        .param("action", "draft"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/drafts"));
    }

    /* =====================================================================
     * Edit list — requires auth + ownership
     * ===================================================================== */

    @Test
    void testEditListForm_Unauthenticated_Redirects() throws Exception {
        mockMvc.perform(get("/lists/" + UUID.randomUUID() + "/edit"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testEditListForm_OwnerSuccess() throws Exception {
        when(userRepository.findFirstByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(listService.getListById(testList.getListId())).thenReturn(Optional.of(testList));
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

        mockMvc.perform(get("/lists/" + testList.getListId() + "/edit")
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(view().name("create-edit-list"));
    }

    @Test
    void testEditList_OwnerSuccess() throws Exception {
        when(userRepository.findFirstByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(listService.getListById(testList.getListId())).thenReturn(Optional.of(testList));
        when(categoryRepository.findById(testCategory.getCategoryId())).thenReturn(Optional.of(testCategory));
        when(listService.saveList(any())).thenReturn(testList);

        mockMvc.perform(post("/lists/" + testList.getListId() + "/edit")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "user@example.com")))
                        .param("title", "Updated Title")
                        .param("categoryId", testCategory.getCategoryId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/" + testList.getListId()));
    }

    @Test
    void testEditList_NonOwner_RedirectsToList() throws Exception {
        User otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setEmail("other@example.com");
        otherUser.setAuthProvider("GOOGLE");
        otherUser.setHasCompletedSetup(true);

        when(userRepository.findFirstByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(listService.getListById(testList.getListId())).thenReturn(Optional.of(testList));

        mockMvc.perform(post("/lists/" + testList.getListId() + "/edit")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> attrs.put("email", "other@example.com")))
                        .param("title", "Hacked Title")
                        .param("categoryId", testCategory.getCategoryId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/" + testList.getListId()));
    }
}

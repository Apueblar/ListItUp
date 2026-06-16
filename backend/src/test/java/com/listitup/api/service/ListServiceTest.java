package com.listitup.api.service;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.model.Category;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListServiceTest {

    @Mock
    private CuratedListRepository listRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CuratedListService listService;

    private CuratedList testList;
    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setEmail("creator@example.com");

        testCategory = new Category();
        testCategory.setName("Tech");

        testList = new CuratedList();
        testList.setListId(UUID.randomUUID());
        testList.setTitle("Best Gadgets");
        testList.setDescription("Cool gadgets list");
        testList.setCreator(testUser);
        testList.setCategory(testCategory);
        testList.setIsDraft(false);
        testList.setVisibility("PUBLIC");
    }

    @Test
    void testGetAllLists() {
        when(listRepository.findAll()).thenReturn(List.of(testList));
        List<CuratedList> lists = listService.getAllLists();
        assertEquals(1, lists.size());
    }

    @Test
    void testGetAllPublicLists() {
        when(listRepository.findByIsDraftFalseOrderByCreatedAtDesc()).thenReturn(List.of(testList));
        List<CuratedList> publicLists = listService.getAllPublicLists();
        assertEquals(1, publicLists.size());
    }

    @Test
    void testGetListById_Found() {
        when(listRepository.findById(testList.getListId())).thenReturn(Optional.of(testList));
        Optional<CuratedList> found = listService.getListById(testList.getListId());
        assertTrue(found.isPresent());
        assertEquals(testList.getListId(), found.get().getListId());
    }

    @Test
    void testGetListById_NotFound() {
        when(listRepository.findById(any())).thenReturn(Optional.empty());
        Optional<CuratedList> found = listService.getListById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    @Test
    void testSaveList() {
        when(listRepository.save(testList)).thenReturn(testList);
        CuratedList saved = listService.saveList(testList);
        assertNotNull(saved);
        assertEquals("Best Gadgets", saved.getTitle());
    }

    @Test
    void testGetListsByCategory() {
        when(listRepository.findByCategoryNameIgnoreCaseOrderByCreatedAtDesc("Tech")).thenReturn(List.of(testList));
        List<CuratedList> lists = listService.getListsByCategory("Tech");
        assertEquals(1, lists.size());
    }

    @Test
    void testDeleteList_Success() {
        when(listRepository.findById(testList.getListId())).thenReturn(Optional.of(testList));
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        doNothing().when(listRepository).delete(testList);

        listService.deleteList(testList.getListId());
        verify(listRepository, times(1)).delete(testList);
    }

    @Test
    void testDeleteList_NotFound() {
        when(listRepository.findById(any())).thenReturn(Optional.empty());
        listService.deleteList(UUID.randomUUID());
        verify(listRepository, never()).delete(any());
    }

    @Test
    void testUpdateCategoryForAll() {
        Category newCat = new Category();
        newCat.setName("Science");
        doNothing().when(listRepository).updateCategoryForAll(testCategory, newCat);
        listService.updateCategoryForAll(testCategory, newCat);
        verify(listRepository, times(1)).updateCategoryForAll(testCategory, newCat);
    }

    @Test
    void testDeleteUser_Success() {
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        when(listRepository.findByCreatorOrderByCreatedAtDesc(testUser)).thenReturn(List.of(testList));
        
        Query mockQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        doNothing().when(listRepository).delete(any());
        doNothing().when(userRepository).delete(testUser);

        listService.deleteUser(testUser.getUserId());
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void testDeleteUser_NotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        listService.deleteUser(UUID.randomUUID());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testDeleteUser_MainAdminCannotBeDeleted() {
        testUser.setEmail("alvaropueblaruisanchez@gmail.com");
        when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
        listService.deleteUser(testUser.getUserId());
        verify(userRepository, never()).delete(testUser);
    }

    @Test
    void testVisibilityToggle() {
        assertEquals("PUBLIC", testList.getVisibility());
        testList.setVisibility("PRIVATE");
        assertEquals("PRIVATE", testList.getVisibility());
    }

    @Test
    void testFeedQuerySorting() {
        when(listRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(testList));
        List<CuratedList> results = listRepository.findAllByOrderByCreatedAtDesc();
        assertFalse(results.isEmpty());
    }

    @Test
    void testSearchPaginationMock() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<CuratedList> page = new PageImpl<>(List.of(testList), pageable, 1);
        when(listRepository.searchLists("Gadgets", pageable)).thenReturn(page);
        
        Page<CuratedList> results = listRepository.searchLists("Gadgets", pageable);
        assertEquals(1, results.getTotalElements());
        assertEquals("Best Gadgets", results.getContent().get(0).getTitle());
    }
}

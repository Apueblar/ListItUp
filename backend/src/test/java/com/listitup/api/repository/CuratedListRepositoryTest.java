package com.listitup.api.repository;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class CuratedListRepositoryTest {

    @Autowired
    private CuratedListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User creator;
    private Category techCategory;
    private Category musicCategory;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setEmail("creator@test.com");
        creator.setUsername("creator");
        creator.setAuthProvider("GOOGLE");
        creator = userRepository.save(creator);

        techCategory = new Category();
        techCategory.setName("Technology");
        techCategory = categoryRepository.save(techCategory);

        musicCategory = new Category();
        musicCategory.setName("Music");
        musicCategory = categoryRepository.save(musicCategory);
    }

    private CuratedList createAndSaveList(String title, String description, Category category, boolean isDraft) {
        CuratedList list = new CuratedList();
        list.setTitle(title);
        list.setDescription(description);
        list.setCategory(category);
        list.setCreator(creator);
        list.setIsDraft(isDraft);
        list.setVisibility("PUBLIC");
        return listRepository.save(list);
    }

    @Test
    void testSaveAndFindList() {
        CuratedList list = createAndSaveList("My First List", "Sample Description", techCategory, false);
        assertNotNull(list.getListId());
        
        CuratedList found = listRepository.findById(list.getListId()).orElse(null);
        assertNotNull(found);
        assertEquals("My First List", found.getTitle());
    }

    @Test
    void testSearchLists_MatchesKeyword() {
        createAndSaveList("Java Programming Guide", "Learn Java language features", techCategory, false);
        createAndSaveList("Python Basics", "Introduction to Python programming", techCategory, false);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CuratedList> page = listRepository.searchLists("programming", pageable);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void testSearchLists_NoMatches() {
        createAndSaveList("Vocal Lessons", "How to sing high notes", musicCategory, false);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CuratedList> page = listRepository.searchLists("Rust", pageable);

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void testSearchListsByCategory_MatchesBoth() {
        createAndSaveList("Best Tech Albums", "Tech themed music", musicCategory, false);
        createAndSaveList("Latest Smartphones", "Newest high tech gadgets", techCategory, false);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CuratedList> page = listRepository.searchListsByCategory("tech", "Technology", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Latest Smartphones", page.getContent().get(0).getTitle());
    }

    @Test
    void testSearchListsByCategory_NoMatches() {
        createAndSaveList("Acoustic Hits", "Great acoustic songs", musicCategory, false);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CuratedList> page = listRepository.searchListsByCategory("acoustic", "Technology", pageable);

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void testFindAllPublicSearchable_Pagination() {
        for (int i = 1; i <= 15; i++) {
            createAndSaveList("List #" + i, "Desc #" + i, techCategory, false);
        }
        // Draft lists should be ignored
        createAndSaveList("Draft List", "Draft Desc", techCategory, true);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CuratedList> page = listRepository.findAllPublicSearchable(pageable);

        assertEquals(15, page.getTotalElements());
        assertEquals(10, page.getContent().size());
    }

    @Test
    void testFindByCategoryPublicSearchable_CategoryFilter() {
        createAndSaveList("Coding Bootcamps", "Learn coding", techCategory, false);
        createAndSaveList("Guitar Tabs", "Acoustic chords", musicCategory, false);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CuratedList> page = listRepository.findByCategoryPublicSearchable("Music", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Guitar Tabs", page.getContent().get(0).getTitle());
    }
}

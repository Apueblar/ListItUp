package com.listitup.api.service;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.model.Category;
import com.listitup.api.repository.CuratedListRepository;
import com.listitup.api.repository.UserRepository;
import com.listitup.api.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CuratedListService {

    private final CuratedListRepository listRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    public CuratedListService(CuratedListRepository listRepository,
                              UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              EntityManager entityManager) {
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<CuratedList> getAllLists() {
        return listRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CuratedList> getListById(UUID id) {
        return listRepository.findById(id);
    }

    @Transactional
    public CuratedList saveList(CuratedList list) {
        return listRepository.save(list);
    }
    
    @Transactional(readOnly = true)
    public List<CuratedList> getListsByCategory(String categoryName) {
        return listRepository.findByCategoryNameIgnoreCaseOrderByCreatedAtDesc(categoryName);
    }

    @Transactional
    public CuratedList getOrCreateUnavailableList() {
        User systemUser = userRepository.findByEmail("system@listitup.com")
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("System");
                    u.setEmail("system@listitup.com");
                    u.setRole("STANDARD");
                    u.setAuthProvider("SYSTEM");
                    return userRepository.save(u);
                });

        Category defaultCategory = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Category cat = new Category();
                    cat.setName("General");
                    cat.setIcon("📁");
                    return categoryRepository.save(cat);
                });

        return listRepository.findByTitleContainingIgnoreCase("List no longer available").stream().findFirst()
                .orElseGet(() -> {
                    CuratedList placeholder = new CuratedList();
                    placeholder.setTitle("List no longer available");
                    placeholder.setDescription("This list has been deleted by its creator and is no longer available.");
                    placeholder.setCategory(defaultCategory);
                    placeholder.setCreator(systemUser);
                    placeholder.setVisibility("PRIVATE");
                    return listRepository.save(placeholder);
                });
    }

    @Transactional
    public void deleteList(UUID id) {
        CuratedList listToDelete = listRepository.findById(id).orElse(null);
        if (listToDelete != null) {
            CuratedList unavailableList = getOrCreateUnavailableList();
            
            // Reassign likes
            entityManager.createQuery("DELETE FROM Like l WHERE l.list = :listToDelete AND l.user IN (SELECT l2.user FROM Like l2 WHERE l2.list = :unavailableList)")
                    .setParameter("listToDelete", listToDelete)
                    .setParameter("unavailableList", unavailableList)
                    .executeUpdate();
            entityManager.createQuery("UPDATE Like l SET l.list = :unavailableList WHERE l.list = :listToDelete")
                    .setParameter("unavailableList", unavailableList)
                    .setParameter("listToDelete", listToDelete)
                    .executeUpdate();

            // Reassign saved lists
            entityManager.createQuery("DELETE FROM SavedList s WHERE s.list = :listToDelete AND s.user IN (SELECT s2.user FROM SavedList s2 WHERE s2.list = :unavailableList)")
                    .setParameter("listToDelete", listToDelete)
                    .setParameter("unavailableList", unavailableList)
                    .executeUpdate();
            entityManager.createQuery("UPDATE SavedList s SET s.list = :unavailableList WHERE s.list = :listToDelete")
                    .setParameter("unavailableList", unavailableList)
                    .setParameter("listToDelete", listToDelete)
                    .executeUpdate();

            // Delete comments
            entityManager.createQuery("DELETE FROM Comment c WHERE c.list = :listToDelete")
                    .setParameter("listToDelete", listToDelete)
                    .executeUpdate();

            // Delete reports
            entityManager.createQuery("DELETE FROM Report r WHERE r.targetList = :listToDelete")
                .setParameter("listToDelete", listToDelete)
                .executeUpdate();

            // Delete list
            listRepository.delete(listToDelete);
        }
    }

    @Transactional
    public void updateCategoryForAll(com.listitup.api.model.Category oldCategory, com.listitup.api.model.Category newCategory) {
        listRepository.updateCategoryForAll(oldCategory, newCategory);
    }
}

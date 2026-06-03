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
    public List<CuratedList> getAllPublicLists() {
        return listRepository.findByIsDraftFalseOrderByCreatedAtDesc();
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
    public void deleteList(UUID id) {
        CuratedList listToDelete = listRepository.findById(id).orElse(null);
        if (listToDelete != null) {
            listRepository.delete(listToDelete);
        }
    }

    @Transactional
    public void updateCategoryForAll(com.listitup.api.model.Category oldCategory, com.listitup.api.model.Category newCategory) {
        listRepository.updateCategoryForAll(oldCategory, newCategory);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        if (user.getEmail().equalsIgnoreCase("alvaropueblaruisanchez@gmail.com")) {
            return;
        }

        // 1. Delete all lists owned by this user
        List<CuratedList> userLists = listRepository.findByCreatorOrderByCreatedAtDesc(user);
        for (CuratedList list : userLists) {
            // Delete reports targeting this list
            entityManager.createQuery("DELETE FROM Report r WHERE r.targetList = :list")
                    .setParameter("list", list)
                    .executeUpdate();
            listRepository.delete(list);
        }

        // 2. Delete reports targeting comments by this user
        entityManager.createQuery("DELETE FROM Report r WHERE r.targetComment IN (SELECT c FROM Comment c WHERE c.author = :user)")
                .setParameter("user", user)
                .executeUpdate();

        // 3. Delete comments, likes, saved lists, and notifications of the user
        entityManager.createQuery("DELETE FROM Comment c WHERE c.author = :user")
                .setParameter("user", user)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Like l WHERE l.user = :user")
                .setParameter("user", user)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM SavedList s WHERE s.user = :user")
                .setParameter("user", user)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM Notification n WHERE n.user = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 4. Delete follows
        entityManager.createQuery("DELETE FROM Follow f WHERE f.follower = :user OR f.followee = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 5. Delete reports submitted or reviewed by this user
        entityManager.createQuery("DELETE FROM Report r WHERE r.submittedByUser = :user OR r.reviewedByAdmin = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 6. Finally delete the user record
        userRepository.delete(user);
    }
}

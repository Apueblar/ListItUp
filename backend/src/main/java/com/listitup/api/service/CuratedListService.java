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
            deleteListAndDependents(listToDelete);
        }
    }

    /**
     * Deletes all dependent records for a list (likes, comments, reports, saved lists)
     * and then deletes the list itself. Must be called within a transaction.
     */
    private void deleteListAndDependents(CuratedList list) {
        // Delete reports targeting comments on this list
        entityManager.createQuery(
                "DELETE FROM Report r WHERE r.targetComment IN (SELECT c FROM Comment c WHERE c.list = :list)")
                .setParameter("list", list)
                .executeUpdate();
        // Delete comments on this list
        entityManager.createQuery("DELETE FROM Comment c WHERE c.list = :list")
                .setParameter("list", list)
                .executeUpdate();
        // Delete reports targeting items of this list (items are cascade-deleted with the list)
        entityManager.createQuery(
                "DELETE FROM Report r WHERE r.targetItem IN (SELECT i FROM Item i WHERE i.list = :list)")
                .setParameter("list", list)
                .executeUpdate();
        // Delete likes on this list
        entityManager.createQuery("DELETE FROM Like l WHERE l.list = :list")
                .setParameter("list", list)
                .executeUpdate();
        // Delete saved-list entries for this list
        entityManager.createQuery("DELETE FROM SavedList s WHERE s.list = :list")
                .setParameter("list", list)
                .executeUpdate();
        // Delete reports targeting this list
        entityManager.createQuery("DELETE FROM Report r WHERE r.targetList = :list")
                .setParameter("list", list)
                .executeUpdate();
        // Now safe to delete the list (items cascade via @OneToMany CascadeType.ALL)
        listRepository.delete(list);
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

        // 1. Delete all lists owned by this user (with all their dependents)
        List<CuratedList> userLists = listRepository.findByCreatorOrderByCreatedAtDesc(user);
        for (CuratedList list : userLists) {
            deleteListAndDependents(list);
        }

        // 2. Delete reports targeting comments by this user (on other people's lists)
        entityManager.createQuery(
                "DELETE FROM Report r WHERE r.targetComment IN (SELECT c FROM Comment c WHERE c.author = :user)")
                .setParameter("user", user)
                .executeUpdate();

        // 3. Delete this user's comments on other lists
        entityManager.createQuery("DELETE FROM Comment c WHERE c.author = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 4. Delete this user's likes on other lists
        entityManager.createQuery("DELETE FROM Like l WHERE l.user = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 5. Delete this user's saved lists
        entityManager.createQuery("DELETE FROM SavedList s WHERE s.user = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 6. Delete notifications for this user
        entityManager.createQuery("DELETE FROM Notification n WHERE n.user = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 7. Delete follows involving this user
        entityManager.createQuery("DELETE FROM Follow f WHERE f.follower = :user OR f.followee = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 8. Delete reports submitted or reviewed by this user
        entityManager.createQuery(
                "DELETE FROM Report r WHERE r.submittedByUser = :user OR r.reviewedByAdmin = :user")
                .setParameter("user", user)
                .executeUpdate();

        // 9. Finally delete the user record
        userRepository.delete(user);
    }
}

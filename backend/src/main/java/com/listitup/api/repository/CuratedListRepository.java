package com.listitup.api.repository;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import com.listitup.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CuratedListRepository extends JpaRepository<CuratedList, UUID> {
    List<CuratedList> findByCreatorOrderByCreatedAtDesc(User creator);
    List<CuratedList> findByCreatorOrderByIsPinnedDescCreatedAtDesc(User creator);
    List<CuratedList> findByCreatorAndIsDraftFalseOrderByIsPinnedDescCreatedAtDesc(User creator);
    List<CuratedList> findByCreatorAndIsDraftTrueOrderByCreatedAtDesc(User creator);
    List<CuratedList> findByTitleContainingIgnoreCase(String title);
    List<CuratedList> findByCategoryNameIgnoreCaseOrderByCreatedAtDesc(String categoryName);
    org.springframework.data.domain.Page<CuratedList> findByCategoryNameIgnoreCase(String categoryName, org.springframework.data.domain.Pageable pageable);
    List<CuratedList> findByIsDraftFalseOrderByCreatedAtDesc();

    // Feed Queries
    List<CuratedList> findAllByOrderByCreatedAtDesc();
    List<CuratedList> findAllByOrderByViewCountDesc();
    List<CuratedList> findByCategoryNameIgnoreCaseOrderByViewCountDesc(String categoryName);
    
    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l JOIN Like k ON k.list = l WHERE k.createdAt > :since AND l.isDraft = false AND l.visibility = 'PUBLIC' AND l.title != 'List no longer available' GROUP BY l HAVING COUNT(k) >= :threshold ORDER BY COUNT(k) DESC, l.createdAt DESC")
    List<CuratedList> findTrendingLists(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since, @org.springframework.data.repository.query.Param("threshold") long threshold);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l JOIN Like k ON k.list = l WHERE k.createdAt > :since AND l.isDraft = false AND l.visibility = 'PUBLIC' AND l.title != 'List no longer available' AND LOWER(l.category.name) = LOWER(:category) GROUP BY l HAVING COUNT(k) >= :threshold ORDER BY COUNT(k) DESC, l.createdAt DESC")
    List<CuratedList> findTrendingListsByCategory(@org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since, @org.springframework.data.repository.query.Param("threshold") long threshold, @org.springframework.data.repository.query.Param("category") String category);
    
    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l WHERE l.creator IN (SELECT uf.followee FROM Follow uf WHERE uf.follower = :follower) ORDER BY l.createdAt DESC")
    List<CuratedList> findListsFromFollowedUsersOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("follower") User follower);
    
    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l WHERE l.category.name = :category AND l.creator IN (SELECT uf.followee FROM Follow uf WHERE uf.follower = :follower) ORDER BY l.createdAt DESC")
    List<CuratedList> findListsFromFollowedUsersByCategoryOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("follower") User follower, @org.springframework.data.repository.query.Param("category") String category);

    // Search Queries
    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l WHERE l.isDraft = false AND l.visibility = 'PUBLIC' AND l.title != 'List no longer available' AND (LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<CuratedList> searchLists(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l WHERE l.isDraft = false AND l.visibility = 'PUBLIC' AND l.title != 'List no longer available' AND LOWER(l.category.name) = LOWER(:category) AND (LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    org.springframework.data.domain.Page<CuratedList> searchListsByCategory(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("category") String category, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l WHERE l.isDraft = false AND l.visibility = 'PUBLIC' AND l.title != 'List no longer available'")
    org.springframework.data.domain.Page<CuratedList> findAllPublicSearchable(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM CuratedList l WHERE l.isDraft = false AND l.visibility = 'PUBLIC' AND l.title != 'List no longer available' AND LOWER(l.category.name) = LOWER(:category)")
    org.springframework.data.domain.Page<CuratedList> findByCategoryPublicSearchable(@org.springframework.data.repository.query.Param("category") String category, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE CuratedList l SET l.category = :newCategory WHERE l.category = :oldCategory")
    void updateCategoryForAll(@org.springframework.data.repository.query.Param("oldCategory") Category oldCategory, @org.springframework.data.repository.query.Param("newCategory") Category newCategory);
}

package com.listitup.api.repository;

import com.listitup.api.model.CuratedList;
import com.listitup.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CuratedListRepository extends JpaRepository<CuratedList, UUID> {
    List<CuratedList> findByCreatorOrderByCreatedAtDesc(User creator);
    List<CuratedList> findByTitleContainingIgnoreCase(String title);
}

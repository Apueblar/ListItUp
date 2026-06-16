package com.listitup.api.repository;

import com.listitup.api.model.SavedList;
import com.listitup.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedListRepository extends JpaRepository<SavedList, UUID> {
    List<SavedList> findByUserOrderBySavedAtDesc(User user);
}

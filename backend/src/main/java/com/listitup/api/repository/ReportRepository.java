package com.listitup.api.repository;

import com.listitup.api.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import com.listitup.api.model.User;
import com.listitup.api.model.CuratedList;
import com.listitup.api.model.Item;
import com.listitup.api.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsBySubmittedByUserAndTargetList(User user, CuratedList targetList);
    boolean existsBySubmittedByUserAndTargetItem(User user, Item targetItem);
    boolean existsBySubmittedByUserAndTargetComment(User user, Comment targetComment);
}

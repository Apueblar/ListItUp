package com.listitup.api.repository;

import com.listitup.api.model.Comment;
import com.listitup.api.model.CuratedList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByListOrderByCreatedAtDesc(CuratedList list);
}

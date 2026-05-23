package com.listitup.api.repository;

import com.listitup.api.model.CategoryProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryProposalRepository extends JpaRepository<CategoryProposal, UUID> {
    List<CategoryProposal> findByStatusOrderByCreatedAtDesc(String status);
}

package com.listitup.api.repository;

import com.listitup.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findFirstByEmail(String email);
    Optional<User> findFirstByUsername(String username);
    Optional<User> findFirstByUsernameAndAuthProvider(String username, String authProvider);
}

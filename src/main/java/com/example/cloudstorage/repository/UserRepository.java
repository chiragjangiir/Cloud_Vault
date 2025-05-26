// src/main/java/com/example/cloudstorage/repository/UserRepository.java
package com.example.cloudstorage.repository;

import com.example.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Spring Data JPA Repository for User entity.
// Provides CRUD operations and custom query methods.
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom method to find a user by username.
    Optional<User> findByUsername(String username);
}


// src/main/java/com/example/cloudstorage/repository/FileMetadataRepository.java
package com.example.cloudstorage.repository;

import com.example.cloudstorage.model.FileMetadata;
import com.example.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Spring Data JPA Repository for FileMetadata entity.
// Provides CRUD operations and custom query methods.
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    // Custom method to find all file metadata owned by a specific user.
    List<FileMetadata> findByOwner(User owner);
}
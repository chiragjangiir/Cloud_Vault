// src/main/java/com/example/cloudstorage/service/StorageService.java
package com.example.cloudstorage.service;

import com.example.cloudstorage.StorageProperties;
import com.example.cloudstorage.model.FileMetadata;
import com.example.cloudstorage.model.User;
import com.example.cloudstorage.repository.FileMetadataRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Service layer for handling file storage operations.
@Service
public class StorageService {

    private final Path rootLocation; // Root directory for storing files
    private final FileMetadataRepository fileMetadataRepository;

    public StorageService(StorageProperties properties, FileMetadataRepository fileMetadataRepository) {
        // Initialize the root storage location based on configured properties
        this.rootLocation = Paths.get(properties.getLocation());
        this.fileMetadataRepository = fileMetadataRepository;
        init(); // Initialize the storage directory
    }

    // Initializes the storage directory if it doesn't exist.
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    // Stores a file and its metadata.
    public FileMetadata store(MultipartFile file, User owner) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            // Generate a unique filename to avoid conflicts
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path destinationFile = this.rootLocation.resolve(
                            Paths.get(uniqueFilename))
                    .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // This is a security check to prevent "path traversal" attacks
                throw new RuntimeException(
                        "Cannot store file outside current directory.");
            }
            // Copy the file to the destination
            Files.copy(file.getInputStream(), destinationFile,
                    StandardCopyOption.REPLACE_EXISTING);

            // Save file metadata to the database
            FileMetadata metadata = new FileMetadata(
                    originalFilename,
                    uniqueFilename,
                    file.getContentType(),
                    file.getSize(),
                    owner
            );
            return fileMetadataRepository.save(metadata);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    // Loads a file as a Resource for download.
    public Resource loadAsResource(String uniqueFilename) {
        try {
            Path file = rootLocation.resolve(uniqueFilename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException(
                        "Could not read file: " + uniqueFilename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + uniqueFilename, e);
        }
    }

    // Deletes a file and its metadata.
    public void delete(Long fileId, User currentUser) {
        Optional<FileMetadata> optionalMetadata = fileMetadataRepository.findById(fileId);
        if (optionalMetadata.isPresent()) {
            FileMetadata metadata = optionalMetadata.get();
            // Ensure the current user is the owner of the file before deleting
            if (!metadata.getOwner().getId().equals(currentUser.getId())) {
                throw new SecurityException("User is not authorized to delete this file.");
            }

            try {
                Path filePath = rootLocation.resolve(metadata.getUniqueFilename());
                Files.deleteIfExists(filePath); // Delete file from disk
                fileMetadataRepository.delete(metadata); // Delete metadata from DB
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete file: " + metadata.getFilename(), e);
            }
        } else {
            throw new RuntimeException("File not found with ID: " + fileId);
        }
    }

    // Retrieves all file metadata for a given user.
    public List<FileMetadata> getFilesByUser(User owner) {
        return fileMetadataRepository.findByOwner(owner);
    }
}
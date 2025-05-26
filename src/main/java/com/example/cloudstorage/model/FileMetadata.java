// src/main/java/com/example/cloudstorage/model/FileMetadata.java
package com.example.cloudstorage.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// JPA Entity representing metadata for a stored file.
@Entity
@Table(name = "file_metadata") // Define table name
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrementing ID
    private Long id;
    private String filename;
    private String uniqueFilename; // To prevent naming conflicts on disk
    private String contentType;
    private Long size;
    private LocalDateTime uploadDate;

    @ManyToOne // Many FileMetadata entries can belong to one User
    @JoinColumn(name = "user_id", nullable = false) // Foreign key to User table
    private User owner;

    // Default constructor for JPA
    public FileMetadata() {
    }

    public FileMetadata(String filename, String uniqueFilename, String contentType, Long size, User owner) {
        this.filename = filename;
        this.uniqueFilename = uniqueFilename;
        this.contentType = contentType;
        this.size = size;
        this.owner = owner;
        this.uploadDate = LocalDateTime.now(); // Set upload date automatically
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUniqueFilename() {
        return uniqueFilename;
    }

    public void setUniqueFilename(String uniqueFilename) {
        this.uniqueFilename = uniqueFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
// src/main/java/com/example/cloudstorage/config/StorageProperties.java
package com.example.cloudstorage;

import org.springframework.boot.context.properties.ConfigurationProperties;

// This class holds configuration properties related to file storage.
// It maps properties prefixed with "storage" from application.properties.
@ConfigurationProperties("storage")
public class StorageProperties {

    /**
     * Folder location for storing files.
     */
    private String location = "uploads"; // Default storage location

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
package com.cloudvault.service;

import com.cloudvault.domain.StorageLocation;
import com.cloudvault.repository.StorageLocationRepository;
import com.cloudvault.service.storage.StorageProvider;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages real storage locations: administrator-provided paths that are
 * validated (exists, directory, readable, writable, sufficient capacity,
 * live read/write probe) before registration, and health-checked afterwards.
 *
 * <p>Cloud Vault never formats, partitions or otherwise mutates devices; it
 * only operates inside explicitly configured directories.</p>
 */
@Service
public class StorageLocationService {

    private static final Logger log = LoggerFactory.getLogger(StorageLocationService.class);

    private final StorageLocationRepository repo;
    private final StorageObjectUsage storageObjectUsage;
    private final StorageProvider provider;

    public StorageLocationService(StorageLocationRepository repo,
                                  StorageObjectUsage storageObjectUsage,
                                  StorageProvider provider) {
        this.repo = repo;
        this.storageObjectUsage = storageObjectUsage;
        this.provider = provider;
    }

    public record ValidationResult(boolean valid, String message, long totalCapacity, long usableCapacity, String filesystem) {}

    /** Full validation of a candidate location including a live write/read probe. */
    public ValidationResult validate(String rawPath, long minFreeBytes) {
        if (rawPath == null || rawPath.isBlank()) {
            return new ValidationResult(false, "Path is required", 0, 0, null);
        }
        Path path;
        try {
            path = Paths.get(rawPath.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return new ValidationResult(false, "Invalid path: " + rawPath, 0, 0, null);
        }
        if (!Files.exists(path)) {
            return new ValidationResult(false, "Path does not exist: " + path, 0, 0, null);
        }
        if (!Files.isDirectory(path)) {
            return new ValidationResult(false, "Path is not a directory: " + path, 0, 0, null);
        }
        if (!Files.isReadable(path)) {
            return new ValidationResult(false, "Directory is not readable: " + path, 0, 0, null);
        }
        if (!Files.isWritable(path)) {
            return new ValidationResult(false, "Directory is not writable: " + path, 0, 0, null);
        }
        long total;
        long usable;
        String fs;
        try {
            FileStore store = Files.getFileStore(path);
            total = store.getTotalSpace();
            usable = store.getUsableSpace();
            fs = describeFilesystem(store);
        } catch (IOException e) {
            return new ValidationResult(false, "Cannot stat filesystem: " + e.getMessage(), 0, 0, null);
        }
        if (minFreeBytes > 0 && usable < minFreeBytes) {
            return new ValidationResult(false, "Insufficient free space: " + usable + " bytes available, " + minFreeBytes + " required", total, usable, fs);
        }
        // Live probe: create, read back and delete a real file.
        Path probe = path.resolve(".cloudvault-health-" + System.nanoTime());
        try {
            byte[] payload = ("cloudvault-health-" + System.currentTimeMillis()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(probe, payload);
            byte[] readBack = Files.readAllBytes(probe);
            if (!java.util.Arrays.equals(payload, readBack)) {
                return new ValidationResult(false, "Read-back verification failed", total, usable, fs);
            }
        } catch (IOException e) {
            return new ValidationResult(false, "Read/write probe failed: " + e.getMessage(), total, usable, fs);
        } finally {
            try {
                Files.deleteIfExists(probe);
            } catch (IOException e) {
                log.warn("Failed to delete health probe file {}", probe);
            }
        }
        return new ValidationResult(true, "OK", total, usable, fs);
    }

    private static String describeFilesystem(FileStore store) {
        try {
            String type = store.type();
            String name = store.name();
            if (type == null || type.isBlank()) return name;
            return name + " (" + type + ")";
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Transactional
    public StorageLocation register(String name, String rawPath, long minFreeBytes) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{2,64}")) {
            throw ApiException.unprocessable("Location name must be 2-64 characters (letters, digits, dash, underscore)");
        }
        if (repo.findByNameIgnoreCase(name).isPresent()) {
            throw ApiException.conflict("A storage location with that name already exists");
        }
        ValidationResult result = validate(rawPath, minFreeBytes);
        if (!result.valid()) {
            throw ApiException.unprocessable("Storage location rejected: " + result.message());
        }
        StorageLocation loc = new StorageLocation();
        loc.setName(name);
        loc.setRootPath(Paths.get(rawPath.trim()).toAbsolutePath().normalize().toString());
        loc.setStatus(StorageLocation.Status.ONLINE);
        loc.setTotalCapacityBytes(result.totalCapacity());
        loc.setUsableCapacityBytes(result.usableCapacity());
        loc.setFilesystem(result.filesystem());
        loc.setWritable(true);
        loc.setAccessMode("RW");
        loc.setLastHealthCheck(Instant.now());
        loc.setLastHealthMessage("Registered");
        return repo.save(loc);
    }

    /**
     * Runs a real health check: path availability, read access, write access,
     * capacity. Updates status ONLINE/OFFLINE based on the actual result —
     * never based on what the database previously claimed.
     */
    @Transactional
    public StorageLocation healthCheck(long locationId) {
        StorageLocation loc = repo.findById(locationId)
                .orElseThrow(() -> ApiException.notFound("Storage location not found"));
        ValidationResult result = validate(loc.getRootPath(), 0);
        loc.setLastHealthCheck(Instant.now());
        if (result.valid()) {
            boolean wasOffline = loc.getStatus() == StorageLocation.Status.OFFLINE;
            loc.setStatus(StorageLocation.Status.ONLINE);
            loc.setWritable(true);
            loc.setTotalCapacityBytes(result.totalCapacity());
            loc.setUsableCapacityBytes(result.usableCapacity());
            loc.setFilesystem(result.filesystem());
            loc.setLastHealthMessage("Healthy");
            if (wasOffline) {
                log.info("Storage location {} is back ONLINE", loc.getName());
            }
        } else {
            boolean wasOnline = loc.getStatus() == StorageLocation.Status.ONLINE;
            loc.setStatus(StorageLocation.Status.OFFLINE);
            loc.setWritable(false);
            loc.setLastHealthMessage(result.message());
            if (wasOnline) {
                log.warn("Storage location {} went OFFLINE: {}", loc.getName(), result.message());
            }
        }
        return repo.save(loc);
    }

    @Transactional
    public int healthCheckAll() {
        List<StorageLocation> all = repo.findAllOrdered();
        for (StorageLocation loc : all) {
            healthCheck(loc.getId());
        }
        return all.size();
    }

    public Optional<StorageLocation> find(long id) {
        return repo.findById(id);
    }

    public Optional<StorageLocation> findByName(String name) {
        return repo.findByNameIgnoreCase(name);
    }

    public List<StorageLocation> all() {
        return repo.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public List<StorageLocationView> allWithUsage() {
        List<StorageLocationView> views = new ArrayList<>();
        for (StorageLocation loc : repo.findAllOrdered()) {
            long used = storageObjectUsage.usedBytesForLocation(loc.getId());
            views.add(new StorageLocationView(loc, used));
        }
        return views;
    }

    /** A location plus its real, database-tracked object usage. */
    public record StorageLocationView(StorageLocation location, long usedBytes) {}

    /**
     * Ensures the auto-registered location from STORAGE_ROOT exists and is
     * healthy. Called at startup.
     */
    @Transactional
    public void ensureDefaultLocation(String rootPath) {
        Path normalized = Paths.get(rootPath).toAbsolutePath().normalize();
        // The configured STORAGE_ROOT is the application's own directory: create
        // it when missing (admin-registered locations are never auto-created).
        if (!Files.exists(normalized)) {
            try {
                Files.createDirectories(normalized);
                log.info("Created STORAGE_ROOT directory {}", normalized);
            } catch (IOException e) {
                log.error("Could not create STORAGE_ROOT '{}': {}", normalized, e.getMessage());
                return;
            }
        }
        Optional<StorageLocation> existing = repo.findAllOrdered().stream()
                .filter(StorageLocation::isAutoRegistered)
                .findFirst();
        if (existing.isPresent()) {
            StorageLocation loc = existing.get();
            if (!loc.getRootPath().equals(normalized.toString())) {
                log.warn("STORAGE_ROOT changed ({} -> {}); re-running validation", loc.getRootPath(), normalized);
                loc.setRootPath(normalized.toString());
                repo.save(loc);
            }
            healthCheck(loc.getId());
            return;
        }
        ValidationResult result = validate(normalized.toString(), 0);
        if (!result.valid()) {
            log.error("STORAGE_ROOT '{}' failed validation: {}", normalized, result.message());
            return;
        }
        StorageLocation loc = new StorageLocation();
        loc.setName("PRIMARY");
        loc.setRootPath(normalized.toString());
        loc.setAutoRegistered(true);
        loc.setStatus(StorageLocation.Status.ONLINE);
        loc.setTotalCapacityBytes(result.totalCapacity());
        loc.setUsableCapacityBytes(result.usableCapacity());
        loc.setFilesystem(result.filesystem());
        loc.setWritable(true);
        loc.setAccessMode("RW");
        loc.setLastHealthCheck(Instant.now());
        loc.setLastHealthMessage("Auto-registered from STORAGE_ROOT");
        repo.save(loc);
        log.info("Auto-registered storage location PRIMARY at {}", normalized);
    }

    @Transactional
    public void disable(long locationId) {
        StorageLocation loc = repo.findById(locationId)
                .orElseThrow(() -> ApiException.notFound("Storage location not found"));
        long used = storageObjectUsage.usedBytesForLocation(loc.getId());
        if (used > 0) {
            throw ApiException.conflict("Location still contains " + used + " bytes of data; migrate it first");
        }
        loc.setStatus(StorageLocation.Status.DISABLED);
        repo.save(loc);
    }
}

package com.cloudvault.service;

import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.StorageObject;
import com.cloudvault.domain.StoragePool;
import com.cloudvault.repository.StorageLocationRepository;
import com.cloudvault.repository.StorageObjectRepository;
import com.cloudvault.service.storage.StorageProvider;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * Physical object operations with real availability checks. Reads and writes
 * fail loudly (503) when the hosting storage location is offline — files on a
 * disconnected drive never "pretend" to work.
 */
@Service
public class ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(ObjectStorageService.class);

    private final StorageObjectRepository objectRepo;
    private final StorageLocationRepository locationRepo;
    private final StorageProvider provider;

    public ObjectStorageService(StorageObjectRepository objectRepo,
                                StorageLocationRepository locationRepo,
                                StorageProvider provider) {
        this.objectRepo = objectRepo;
        this.locationRepo = locationRepo;
        this.provider = provider;
    }

    /** Streams the object into storage (no transaction — may be large). */
    public StorageProvider.WriteResult writePhysical(StorageLocation location, String storageKey, InputStream in) {
        requireAvailable(location);
        try {
            return provider.write(location, storageKey, in);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "STORAGE_WRITE_FAILED", "Failed to write to storage: " + e.getMessage());
        }
    }

    /** Persists object metadata; caller must be inside a transaction. */
    public StorageObject persist(StoragePool pool, StorageLocation location, String storageKey,
                                 StorageProvider.WriteResult result, String contentType) {
        StorageObject obj = new StorageObject();
        obj.setPool(pool);
        obj.setLocation(location);
        obj.setStorageKey(storageKey);
        obj.setPhysicalReference(result.physicalReference());
        obj.setSizeBytes(result.sizeBytes());
        obj.setChecksumSha256(result.checksumSha256());
        obj.setContentType(contentType);
        obj.setVerifiedAt(Instant.now());
        return objectRepo.save(obj);
    }

    /** Opens a verified read stream, or throws 503 when storage is unavailable. */
    public InputStream openRead(StorageObject obj, long offset, long length) {
        StorageLocation location = obj.getLocation();
        requireAvailable(location);
        if (!provider.exists(location, obj.getStorageKey())) {
            throw ApiException.storageUnavailable("Storage object missing on " + location.getName());
        }
        try {
            return provider.openRead(location, obj.getStorageKey(), offset, length);
        } catch (IOException e) {
            throw ApiException.storageUnavailable("Storage temporarily unavailable: " + e.getMessage());
        }
    }

    public void deletePhysical(StorageObject obj) throws IOException {
        provider.delete(obj.getLocation(), obj.getStorageKey());
    }

    /** Best-effort physical delete by raw key (failed-upload cleanup). */
    public void deleteByKeyQuietly(com.cloudvault.domain.StorageLocation location, String storageKey) {
        try {
            provider.delete(location, storageKey);
        } catch (IOException e) {
            log.warn("Failed to clean up physical object {} on {}: {}", storageKey, location.getName(), e.getMessage());
        }
    }

    /** Removes object metadata rows; caller must be inside a transaction. */
    @Transactional
    public void deleteMetadata(java.util.List<Long> objectIds) {
        objectRepo.deleteAllById(objectIds);
    }

    public StorageObject require(long objectId) {
        return objectRepo.findById(objectId)
                .orElseThrow(() -> ApiException.notFound("Storage object not found"));
    }

    /** Points an object row at its new location after a verified migration copy. */
    @Transactional
    public void updateObjectLocation(long objectId, long destinationId, String newKey, String physicalReference) {
        StorageObject obj = objectRepo.findById(objectId)
                .orElseThrow(() -> new IllegalStateException("Object disappeared during migration"));
        StorageLocation destination = locationRepo.findById(destinationId)
                .orElseThrow(() -> new IllegalStateException("Destination disappeared during migration"));
        obj.setLocation(destination);
        obj.setStorageKey(newKey);
        obj.setPhysicalReference(physicalReference);
        obj.setVerifiedAt(Instant.now());
        objectRepo.save(obj);
    }

    public boolean exists(StorageObject obj) {
        return provider.exists(obj.getLocation(), obj.getStorageKey());
    }

    public String checksum(StorageObject obj) throws IOException {
        try (InputStream in = openRead(obj, 0, -1)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) digest.update(buf, 0, n);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private void requireAvailable(StorageLocation location) {
        StorageLocation fresh = locationRepo.findById(location.getId()).orElse(location);
        if (fresh.getStatus() == StorageLocation.Status.OFFLINE) {
            throw ApiException.storageUnavailable("Storage temporarily unavailable (" + fresh.getName() + " is offline)");
        }
        if (fresh.getStatus() == StorageLocation.Status.DISABLED) {
            throw ApiException.storageUnavailable("Storage location " + fresh.getName() + " is disabled");
        }
    }

    public StorageProvider provider() {
        return provider;
    }

    @Transactional(readOnly = true)
    public List<StorageObject> byLocation(long locationId) {
        return objectRepo.findByLocationId(locationId);
    }
}

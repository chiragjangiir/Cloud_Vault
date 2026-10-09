package com.cloudvault.service;

import com.cloudvault.domain.BackgroundJob;
import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.StorageObject;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.repository.StorageLocationRepository;
import com.cloudvault.repository.StorageObjectRepository;
import com.cloudvault.service.storage.StorageProvider;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Storage migration between registered locations:
 * discover → reserve destination capacity → copy → verify checksum → update
 * metadata → delete source. If any step fails the source stays intact (for
 * objects not yet migrated) and the job reports the real failure. Progress is
 * derived from processed objects and bytes — never simulated.
 */
@Service
public class MigrationService {

    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);
    private static final int PAGE = 200;

    private final StorageLocationRepository locations;
    private final StorageObjectRepository objects;
    private final BackgroundJobRepository jobs;
    private final ObjectStorageService objectStorage;
    private final NotificationService notifications;
    private final AuditService auditService;

    public MigrationService(StorageLocationRepository locations, StorageObjectRepository objects,
                            BackgroundJobRepository jobs, ObjectStorageService objectStorage,
                            NotificationService notifications, AuditService auditService) {
        this.locations = locations;
        this.objects = objects;
        this.jobs = jobs;
        this.objectStorage = objectStorage;
        this.notifications = notifications;
        this.auditService = auditService;
    }

    public record MigrationPreview(long totalObjects, long totalBytes,
                                   long destinationFreeBytes, boolean destinationHasCapacity) {}

    @Transactional(readOnly = true)
    public MigrationPreview preview(long sourceId, long destinationId) {
        StorageLocation source = requireLocation(sourceId);
        StorageLocation destination = requireLocation(destinationId);
        if (source.getId().equals(destination.getId())) {
            throw ApiException.unprocessable("Source and destination must differ");
        }
        long count = objects.countByLocationId(sourceId);
        long bytes = objects.sumSizeByLocationId(sourceId);
        long free = destination.getUsableCapacityBytes();
        return new MigrationPreview(count, bytes, free, free >= bytes);
    }

    /** Executes the migration for the given queued job. */
    public void run(long jobId) {
        BackgroundJob job = jobs.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
        Map<String, Object> payload = Json.readMap(job.getPayload());
        long sourceId = ((Number) payload.get("sourceId")).longValue();
        long destinationId = ((Number) payload.get("destinationId")).longValue();

        StorageLocation source = requireLocation(sourceId);
        StorageLocation destination = requireLocation(destinationId);
        if (source.getId().equals(destination.getId())) {
            throw new IllegalStateException("Source and destination must differ");
        }
        if (destination.getStatus() != StorageLocation.Status.ONLINE || !destination.isWritable()) {
            throw new IllegalStateException("Destination storage is not available: " + destination.getLastHealthMessage());
        }

        long totalObjects = objects.countByLocationId(sourceId);
        long totalBytes = objects.sumSizeByLocationId(sourceId);
        updateProgress(jobId, 0, totalObjects, 0, totalBytes);

        long processed = 0;
        long bytesDone = 0;
        while (true) {
            Page<StorageObject> batch = objects.findAll(
                    org.springframework.data.jpa.domain.Specification.<StorageObject>where(
                            (root, q, cb) -> cb.equal(root.get("location").get("id"), sourceId)),
                    PageRequest.of(0, PAGE, Sort.by("id")));
            if (batch.isEmpty()) break;
            for (StorageObject obj : batch) {
                migrateOne(obj, destination);
                processed++;
                bytesDone += obj.getSizeBytes();
                updateProgress(jobId, processed, totalObjects, bytesDone, totalBytes);
            }
            // Always re-query page 0: migrated objects leave the source set, so the
            // next batch is the next unmigrated slice. Incrementing the page while
            // removing items from the filtered result would silently skip objects
            // once a location holds more than PAGE rows.
        }

        auditService.record("STORAGE_MIGRATION_COMPLETED", "STORAGE_LOCATION", String.valueOf(destinationId),
                processed + " objects (" + bytesDone + " bytes) migrated from " + source.getName() + " to " + destination.getName());
        notifyAdmins(NotificationService.MIGRATION_COMPLETED, "Storage migration completed",
                processed + " objects (" + Format.bytes(bytesDone) + ") migrated from " + source.getName()
                        + " to " + destination.getName() + ".");
    }

    /** Copies one object: copy → checksum verify → metadata update → source delete. */
    private void migrateOne(StorageObject obj, StorageLocation destination) {
        String newKey = objectStorage.provider().newStorageKey();
        StorageProvider.WriteResult written;
        try (InputStream in = objectStorage.openRead(obj, 0, -1)) {
            written = objectStorage.writePhysical(destination, newKey, in);
        } catch (IOException | RuntimeException e) {
            objectStorage.deleteByKeyQuietly(destination, newKey);
            throw new IllegalStateException("Copy failed for object " + obj.getStorageKey() + ": " + e.getMessage(), e);
        }
        if (!written.checksumSha256().equalsIgnoreCase(obj.getChecksumSha256())) {
            objectStorage.deleteByKeyQuietly(destination, newKey);
            throw new IllegalStateException("Checksum mismatch while migrating object " + obj.getStorageKey()
                    + " — source left intact");
        }
        // Metadata switch (its own transaction), then delete the source copy.
        objectStorage.updateObjectLocation(obj.getId(), destination.getId(), newKey, written.physicalReference());
        try {
            objectStorage.deletePhysical(obj);
        } catch (IOException e) {
            // The object now lives at the destination; a leftover source file is
            // reported as an orphan by the storage audit. Never silent.
            log.error("Migrated object {} but failed to remove source file {}: {}",
                    obj.getStorageKey(), obj.getPhysicalReference(), e.getMessage());
        }
    }

    private void updateProgress(long jobId, long processed, long total, long bytes, long bytesTotal) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setProgressProcessed(processed);
            j.setProgressTotal(total);
            j.setBytesProcessed(bytes);
            j.setBytesTotal(bytesTotal);
            jobs.save(j);
        });
    }

    private StorageLocation requireLocation(long id) {
        return locations.findById(id).orElseThrow(() -> ApiException.notFound("Storage location not found"));
    }

    private void notifyAdmins(String type, String title, String body) {
        notifications.notifyAdmins(type, title, body);
    }
}

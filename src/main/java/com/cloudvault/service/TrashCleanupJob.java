package com.cloudvault.service;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.Folder;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.repository.FileEntryRepository;
import com.cloudvault.repository.FolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Permanently deletes trash items whose retention window has expired:
 * physical objects first, then metadata, then quota release.
 */
@Component
public class TrashCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TrashCleanupJob.class);

    private final FileEntryRepository files;
    private final FolderRepository folders;
    private final FileService fileService;
    private final BackgroundJobRepository jobs;
    private final NotificationService notifications;
    private final com.cloudvault.repository.UserRepository users;

    public TrashCleanupJob(FileEntryRepository files, FolderRepository folders, FileService fileService,
                           BackgroundJobRepository jobs, NotificationService notifications,
                           com.cloudvault.repository.UserRepository users) {
        this.files = files;
        this.folders = folders;
        this.fileService = fileService;
        this.jobs = jobs;
        this.notifications = notifications;
        this.users = users;
    }

    public void run(long jobId) {
        Instant now = Instant.now();
        List<FileEntry> expiredFiles = new ArrayList<>(files.findTrashExpired(now));
        List<Folder> expiredFolders = new ArrayList<>(folders.findAll()).stream()
                .filter(Folder::isTrashed)
                .filter(f -> f.getRetentionUntil() != null && f.getRetentionUntil().isBefore(now))
                .toList();

        long totalObjects = expiredFiles.size() + expiredFolders.size();
        long totalBytes = expiredFiles.stream().mapToLong(FileEntry::getSizeBytes).sum();
        update(jobId, 0, totalObjects, 0, totalBytes);

        long processed = 0;
        long bytes = 0;
        for (FileEntry file : expiredFiles) {
            try {
                long size = file.getSizeBytes();
                Long owner = file.getOwner().getId();
                fileService.purgeInternal(file);
                bytes += size;
                notifyOwner(owner, "TRASH_PURGED",
                        "File permanently deleted",
                        "\"" + file.getName() + "\" was permanently deleted after its retention period expired.");
            } catch (Exception e) {
                log.error("Trash cleanup failed for file {}: {}", file.getId(), e.getMessage());
                markFailed(jobId, "Failed to purge file " + file.getId() + ": " + e.getMessage());
                return;
            }
            processed++;
            update(jobId, processed, totalObjects, bytes, totalBytes);
        }

        // Expired folder subtrees: purge contained files first, then folder rows.
        for (Folder folder : expiredFolders) {
            try {
                List<FileEntry> contained = files.findByFolderIdIn(folders.findSubtreeIds(folder.getId()));
                for (FileEntry containedFile : contained) {
                    long size = containedFile.getSizeBytes();
                    Long owner = containedFile.getOwner().getId();
                    fileService.purgeInternal(containedFile);
                    bytes += size;
                    notifyOwner(owner, NotificationService.TRASH_PURGED,
                            "Folder permanently deleted",
                            "\"" + containedFile.getName() + "\" was permanently deleted after its retention period expired.");
                    processed++;
                    update(jobId, processed, totalObjects, bytes, totalBytes);
                }
                List<Long> subtree = folders.findSubtreeIds(folder.getId());
                folders.deleteAll(folders.findAllById(subtree));
            } catch (Exception e) {
                log.error("Trash cleanup failed for folder {}: {}", folder.getId(), e.getMessage());
                markFailed(jobId, "Failed to purge folder " + folder.getId() + ": " + e.getMessage());
                return;
            }
            processed++;
            update(jobId, processed, totalObjects, bytes, totalBytes);
        }
    }

    private void notifyOwner(Long userId, String type, String title, String body) {
        if (userId == null) return;
        users.findById(userId).ifPresent(u -> notifications.notify(u, type, title, body, "/app/trash"));
    }

    private void update(long jobId, long processed, long total, long bytes, long bytesTotal) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setProgressProcessed(processed);
            j.setProgressTotal(total);
            j.setBytesProcessed(bytes);
            j.setBytesTotal(bytesTotal);
            jobs.save(j);
        });
    }

    private void markFailed(long jobId, String message) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setErrorMessage(message.length() > 2000 ? message.substring(0, 2000) : message);
            jobs.save(j);
        });
        throw new IllegalStateException(message);
    }
}

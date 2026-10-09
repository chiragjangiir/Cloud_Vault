package com.cloudvault.repository;

import com.cloudvault.domain.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileEntryRepository extends JpaRepository<FileEntry, Long>, JpaSpecificationExecutor<FileEntry> {

    @Query("select f from FileEntry f join fetch f.folder where f.id = :id")
    Optional<FileEntry> findByIdWithFolder(@Param("id") long id);

    @Query("select f from FileEntry f where f.owner.id = :ownerId and f.folder.id = :folderId and f.status = 'ACTIVE' order by lower(f.name)")
    List<FileEntry> findLiveInFolder(@Param("ownerId") long ownerId, @Param("folderId") long folderId);

    @Query("select f from FileEntry f where f.owner.id = :ownerId and f.status = 'ACTIVE' order by f.updatedAt desc")
    List<FileEntry> findLiveByOwnerRecent(@Param("ownerId") long ownerId);

    @Query("select coalesce(sum(f.sizeBytes), 0) from FileEntry f where f.owner.id = :ownerId")
    long sumSizeByOwner(@Param("ownerId") long ownerId);

    long countByOwnerId(Long ownerId);

    long countByStatus(FileEntry.Status status);

    @Query("select coalesce(sum(f.sizeBytes), 0) from FileEntry f where f.status = 'ACTIVE'")
    long sumActiveSize();

    long countByOwnerIdAndStatus(Long ownerId, FileEntry.Status status);

    @Query("select f from FileEntry f where f.owner.id = :ownerId and f.status = 'TRASHED' order by f.deletedAt desc")
    List<FileEntry> findTrashedByOwnerOrdered(@Param("ownerId") long ownerId);

    @Query("select coalesce(sum(f.sizeBytes), 0) from FileEntry f where f.status = 'TRASHED'")
    long sumTrashedSize();

    @Query("select f from FileEntry f where f.status = 'TRASHED' and f.retentionUntil is not null and f.retentionUntil < :now")
    List<FileEntry> findTrashExpired(@Param("now") java.time.Instant now);

    @Query("select f from FileEntry f where f.status = 'TRASHED' and f.retentionUntil is null")
    List<FileEntry> findTrashWithoutRetention();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE files SET status = 'TRASHED', deleted_at = :now, retention_until = :retention,
                             original_folder_id = NULL
            WHERE folder_id IN (:ids) AND status = 'ACTIVE'""", nativeQuery = true)
    int markSubtreeTrashed(@Param("ids") List<Long> folderIds, @Param("now") java.time.Instant now,
                           @Param("retention") java.time.Instant retention);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE files SET status = 'ACTIVE', deleted_at = NULL, retention_until = NULL
            WHERE folder_id IN (:ids) AND status = 'TRASHED' AND original_folder_id IS NULL""", nativeQuery = true)
    int markSubtreeActive(@Param("ids") List<Long> folderIds);

    List<FileEntry> findByFolderIdIn(List<Long> folderIds);
}

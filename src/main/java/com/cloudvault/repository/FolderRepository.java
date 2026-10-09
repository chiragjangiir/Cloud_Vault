package com.cloudvault.repository;

import com.cloudvault.domain.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("select f from Folder f where f.owner.id = :ownerId and f.parent.id = :parentId and f.deletedAt is null order by lower(f.name)")
    List<Folder> findLiveChildren(@Param("ownerId") long ownerId, @Param("parentId") Long parentId);

    @Query("select f from Folder f where f.owner.id = :ownerId and f.parent is null and f.deletedAt is null order by lower(f.name)")
    List<Folder> findLiveRoots(@Param("ownerId") long ownerId);

    @Query("select f from Folder f where f.owner.id = :ownerId and f.id = :id and f.deletedAt is null")
    Optional<Folder> findLiveById(@Param("ownerId") long ownerId, @Param("id") long id);

    @Query(value = """
            WITH RECURSIVE subtree AS (
                SELECT id FROM folders WHERE id = :rootId
                UNION ALL
                SELECT f.id FROM folders f JOIN subtree s ON f.parent_id = s.id
            ) SELECT id FROM subtree""", nativeQuery = true)
    List<Long> findSubtreeIds(@Param("rootId") long rootId);

    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT id, parent_id FROM folders WHERE id = :folderId
                UNION ALL
                SELECT f.id, f.parent_id FROM folders f JOIN ancestors a ON f.id = a.parent_id
            ) SELECT id FROM ancestors""", nativeQuery = true)
    List<Long> findAncestorIds(@Param("folderId") long folderId);

    @Query("select f from Folder f where f.owner.id = :ownerId and f.deletedAt is not null and f.parent is null order by f.deletedAt desc")
    List<Folder> findTrashedRoots(@Param("ownerId") long ownerId);

    @Modifying
    @Query("update Folder f set f.deletedAt = :now, f.retentionUntil = :retention, f.originalParentId = coalesce(f.parent.id, null) where f.id in :ids")
    int markTrashed(@Param("ids") List<Long> ids, @Param("now") java.time.Instant now, @Param("retention") java.time.Instant retention);

    @Modifying
    @Query("update Folder f set f.deletedAt = null, f.retentionUntil = null where f.id in :ids and f.deletedAt is not null")
    int clearTrashed(@Param("ids") List<Long> ids);

    long countByOwnerId(Long ownerId);

    List<Folder> findByOwnerIdAndDeletedAtIsNullOrderByNameAsc(Long ownerId);
}

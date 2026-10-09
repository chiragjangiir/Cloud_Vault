package com.cloudvault.repository;

import com.cloudvault.domain.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findByFileIdOrderByVersionNumberDesc(long fileId);

    Optional<FileVersion> findByFileIdAndVersionNumber(long fileId, int versionNumber);

    long countByFileId(long fileId);

    @Query("select coalesce(sum(v.sizeBytes), 0) from FileVersion v where v.file.id = :fileId")
    long sumSizeByFile(@Param("fileId") long fileId);

    /** Authoritative usage: every stored version owned by the user (trash included). */
    @Query("select coalesce(sum(v.sizeBytes), 0) from FileVersion v where v.file.owner.id = :userId")
    long sumSizeByOwner(@Param("userId") long userId);

    @Query("select v from FileVersion v join fetch v.file where v.file.owner.id = :userId order by v.id desc")
    java.util.List<com.cloudvault.domain.FileVersion> findByOwnerRecent(@Param("userId") long userId);
}

package com.cloudvault.repository;

import com.cloudvault.domain.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {

    Optional<Share> findByToken(String token);

    @Query("select s from Share s join fetch s.file join fetch s.owner where s.owner.id = :ownerId order by s.createdAt desc")
    List<Share> findByOwnerIdWithFile(@Param("ownerId") long ownerId);

    @Query("select s from Share s join fetch s.file join fetch s.owner where s.recipient.id = :userId order by s.createdAt desc")
    List<Share> findByRecipientIdWithFile(@Param("userId") long userId);

    @Query("select s from Share s where s.file.id = :fileId")
    List<Share> findByFileId(@Param("fileId") long fileId);

    long countByOwnerIdAndRevokedAtIsNull(Long ownerId);

    @Query("select s from Share s where s.revokedAt is null and s.expiresAt is not null and s.expiresAt < :now")
    List<Share> findExpiredActive(@Param("now") Instant now);
}

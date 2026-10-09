package com.cloudvault.repository;

import com.cloudvault.domain.UserUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserUsageRepository extends JpaRepository<UserUsage, Long> {

    /** Pessimistic row lock — serializes quota mutations per user. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserUsage u where u.userId = :userId")
    Optional<UserUsage> findByIdForUpdate(@Param("userId") long userId);

    /** Ensures a usage row exists. Executed as an update, not a select —
     *  ON CONFLICT DO NOTHING returns no rows. */
    @Modifying
    @Query(value = "INSERT INTO user_usage (user_id, used_bytes, updated_at) VALUES (:userId, 0, now()) ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    void ensureRow(@Param("userId") long userId);
}

package com.cloudvault.repository;

import com.cloudvault.domain.QuotaReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface QuotaReservationRepository extends JpaRepository<QuotaReservation, Long> {

    @Query("select coalesce(sum(r.bytes), 0) from QuotaReservation r where r.user.id = :userId and r.status = 'RESERVED' and r.expiresAt > :now")
    long sumActiveReservations(@Param("userId") long userId, @Param("now") Instant now);

    @Query("select r from QuotaReservation r where r.user.id = :userId and r.status = 'RESERVED' order by r.id")
    List<QuotaReservation> findActiveByUser(@Param("userId") long userId);

    @Query("select r from QuotaReservation r where r.status = 'RESERVED' and r.expiresAt < :now")
    List<QuotaReservation> findExpired(@Param("now") Instant now);
}

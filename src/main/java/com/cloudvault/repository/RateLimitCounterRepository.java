package com.cloudvault.repository;

import com.cloudvault.domain.RateLimitCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface RateLimitCounterRepository extends JpaRepository<RateLimitCounter, RateLimitCounter.RateLimitId> {

    @Modifying(clearAutomatically = true)
    @Query("delete from RateLimitCounter r where r.lastSeenAt < :cutoff")
    int deleteExpired(@Param("cutoff") java.time.Instant cutoff);
}

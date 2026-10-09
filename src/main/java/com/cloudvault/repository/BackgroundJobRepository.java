package com.cloudvault.repository;

import com.cloudvault.domain.BackgroundJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from BackgroundJob j where j.state = 'QUEUED' order by j.id")
    List<BackgroundJob> findQueuedForUpdate();

    List<BackgroundJob> findTop20ByOrderByCreatedAtDesc();

    List<BackgroundJob> findByTypeOrderByCreatedAtDesc(String type);

    long countByState(BackgroundJob.State state);

    long countByTypeAndStateIn(String type, java.util.Collection<BackgroundJob.State> states);

    Optional<BackgroundJob> findTopByStateOrderByIdDesc(BackgroundJob.State state);
}

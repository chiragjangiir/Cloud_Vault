package com.cloudvault.repository;

import com.cloudvault.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // NOTE: the actor parameter must stay OUTSIDE lower(): PostgreSQL cannot
    // infer a typed parameter inside lower() and resolves it to bytea. The
    // caller lowercases the actor instead.
    @Query("select a from AuditLog a where (:action is null or a.action = :action) " +
           "and (:actor is null or lower(a.actorUsername) = :actor) order by a.createdAt desc")
    Page<AuditLog> search(@Param("action") String action, @Param("actor") String actor, Pageable pageable);
}

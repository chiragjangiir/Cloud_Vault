package com.cloudvault.repository;

import com.cloudvault.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findByUserIdOrderByCreatedAtDesc(long userId);

    long countByUserId(long userId);
}

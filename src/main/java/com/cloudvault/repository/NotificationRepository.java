package com.cloudvault.repository;

import com.cloudvault.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(long userId);

    long countByUserIdAndReadAtIsNull(long userId);

    @Query("select n from Notification n where n.user.id = :userId and n.readAt is null")
    List<Notification> findUnread(@Param("userId") long userId);
}

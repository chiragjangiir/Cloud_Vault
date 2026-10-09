package com.cloudvault.repository;

import com.cloudvault.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    @Query("select s from Subscription s join fetch s.user join fetch s.plan order by s.createdAt desc")
    List<Subscription> findAllWithUserAndPlan();

    @Query("select count(s) from Subscription s where s.status = :status")
    long countByStatus(@Param("status") Subscription.Status status);
}

package com.cloudvault.repository;

import com.cloudvault.domain.StorageObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorageObjectRepository extends JpaRepository<StorageObject, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<StorageObject> {

    Optional<StorageObject> findByStorageKey(String storageKey);

    long countByLocationId(Long locationId);

    @Query("select coalesce(sum(o.sizeBytes), 0) from StorageObject o where o.location.id = :locationId")
    long sumSizeByLocationId(@Param("locationId") long locationId);

    @Query("select o from StorageObject o where o.location.id = :locationId order by o.id")
    List<StorageObject> findByLocationId(@Param("locationId") long locationId);

    long countByPoolId(Long poolId);
}

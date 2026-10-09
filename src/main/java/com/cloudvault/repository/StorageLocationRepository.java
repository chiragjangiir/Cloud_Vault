package com.cloudvault.repository;

import com.cloudvault.domain.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    Optional<StorageLocation> findByNameIgnoreCase(String name);

    List<StorageLocation> findByStatusOrderByName(StorageLocation.Status status);

    @Query("select l from StorageLocation l order by l.name")
    List<StorageLocation> findAllOrdered();
}

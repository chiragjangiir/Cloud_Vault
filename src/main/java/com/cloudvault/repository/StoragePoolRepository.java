package com.cloudvault.repository;

import com.cloudvault.domain.StoragePool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoragePoolRepository extends JpaRepository<StoragePool, Long> {

    Optional<StoragePool> findByNameIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Query("select distinct p from StoragePool p left join fetch p.locations where p.id = :id")
    Optional<StoragePool> findWithLocationsById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select distinct p from StoragePool p left join fetch p.locations")
    List<StoragePool> findAllWithLocations();
}

package com.cloudvault.service;

import com.cloudvault.repository.StorageObjectRepository;
import org.springframework.stereotype.Component;

/** Real usage aggregates computed from storage object rows. */
@Component
public class StorageObjectUsage {

    private final StorageObjectRepository repo;

    public StorageObjectUsage(StorageObjectRepository repo) {
        this.repo = repo;
    }

    public long usedBytesForLocation(long locationId) {
        return repo.sumSizeByLocationId(locationId);
    }

    public long objectCountForLocation(long locationId) {
        return repo.countByLocationId(locationId);
    }

    public long totalObjectCount() {
        return repo.count();
    }
}

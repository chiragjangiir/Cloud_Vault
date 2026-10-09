package com.cloudvault.service;

import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.StoragePool;
import com.cloudvault.repository.StorageLocationRepository;
import com.cloudvault.repository.StoragePoolRepository;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Storage pools whose capacity is always computed from the real registered
 * member locations — never hardcoded. Placement picks the healthiest location
 * with enough free space.
 */
@Service
public class StoragePoolService {

    public static final String DEFAULT_POOL = "MAIN_STORAGE";

    private static final Logger log = LoggerFactory.getLogger(StoragePoolService.class);

    private final StoragePoolRepository poolRepo;
    private final StorageLocationRepository locationRepo;
    private final StorageObjectUsage usage;

    public StoragePoolService(StoragePoolRepository poolRepo,
                              StorageLocationRepository locationRepo,
                              StorageObjectUsage usage) {
        this.poolRepo = poolRepo;
        this.locationRepo = locationRepo;
        this.usage = usage;
    }

    public record PoolView(StoragePool pool, long totalCapacityBytes, long usedBytes, long freeBytes,
                           List<LocationMembership> locations) {}

    public record LocationMembership(StorageLocation location, long usedBytes, long objectCount) {}

    /** Ensures the default pool exists and contains every non-disabled location. */
    @Transactional
    public StoragePool ensureDefaultPool() {
        StoragePool pool = loadPoolWithLocations();
        boolean changed = false;
        for (StorageLocation loc : locationRepo.findAllOrdered()) {
            if (loc.getStatus() == StorageLocation.Status.DISABLED) continue;
            boolean present = pool.getLocations().stream().anyMatch(l -> l.getId().equals(loc.getId()));
            if (!present) {
                pool.getLocations().add(loc);
                changed = true;
            }
        }
        if (changed) {
            pool = poolRepo.save(pool);
            log.info("Storage pool {} synchronized with registered locations", DEFAULT_POOL);
        }
        return pool;
    }

    private StoragePool loadPoolWithLocations() {
        StoragePool existing = poolRepo.findByNameIgnoreCase(DEFAULT_POOL).orElse(null);
        if (existing == null) {
            StoragePool p = new StoragePool();
            p.setName(DEFAULT_POOL);
            p.setDescription("Default pool containing all registered storage locations");
            p = poolRepo.saveAndFlush(p);
            existing = p;
        }
        return poolRepo.findWithLocationsById(existing.getId())
                .orElseThrow(() -> new IllegalStateException("Default storage pool missing"));
    }

    @Transactional
    public List<PoolView> allPools() {
        ensureDefaultPool();
        List<PoolView> views = new ArrayList<>();
        for (StoragePool pool : poolRepo.findAllWithLocations()) {
            views.add(toView(pool));
        }
        return views;
    }

    @Transactional
    public PoolView defaultPoolView() {
        return toView(ensureDefaultPool());
    }

    private PoolView toView(StoragePool pool) {
        long total = 0;
        long free = 0;
        List<LocationMembership> members = new ArrayList<>();
        for (StorageLocation loc : pool.getLocations()) {
            long used = usage.usedBytesForLocation(loc.getId());
            members.add(new LocationMembership(loc, used, usage.objectCountForLocation(loc.getId())));
            if (loc.getStatus() == StorageLocation.Status.ONLINE) {
                total += loc.getTotalCapacityBytes();
                free += loc.getUsableCapacityBytes();
            }
        }
        long used = members.stream().mapToLong(LocationMembership::usedBytes).sum();
        return new PoolView(pool, total, used, free, members);
    }

    public record Placement(StoragePool pool, StorageLocation location) {}

    /**
     * Picks the ONLINE, writable location in the default pool with the most
     * free space that can hold {@code bytes}. Throws 507 when no location can.
     */
    @Transactional
    public Placement pick(long bytes) {
        StoragePool pool = ensureDefaultPool();
        StorageLocation best = null;
        long bestFree = -1;
        for (StorageLocation loc : pool.getLocations()) {
            if (loc.getStatus() != StorageLocation.Status.ONLINE) continue;
            if (!loc.isWritable()) continue;
            long free = loc.getUsableCapacityBytes();
            if (free >= bytes && free > bestFree) {
                best = loc;
                bestFree = free;
            }
        }
        if (best == null) {
            throw ApiException.quotaExceeded("Insufficient storage capacity on the server");
        }
        return new Placement(pool, best);
    }

    /** Ranks locations for migration destinations (most free space first). */
    @Transactional(readOnly = true)
    public List<StorageLocation> migrationDestinations(long sourceLocationId) {
        return locationRepo.findAllOrdered().stream()
                .filter(l -> !l.getId().equals(sourceLocationId))
                .filter(l -> l.getStatus() == StorageLocation.Status.ONLINE && l.isWritable())
                .sorted(Comparator.comparingLong(StorageLocation::getUsableCapacityBytes).reversed())
                .toList();
    }
}

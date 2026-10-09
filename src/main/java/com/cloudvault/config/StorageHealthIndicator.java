package com.cloudvault.config;

import com.cloudvault.domain.StorageLocation;
import com.cloudvault.service.StorageLocationService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Real storage health for /actuator/health. Every registered location is
 * stat'ed on disk (exists, directory, readable, writable) — status is never
 * taken from what the database previously claimed. DISABLED locations are
 * excluded; an offline or inaccessible enabled location marks storage DOWN.
 */
@Component("storage")
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageLocationService locations;

    public StorageHealthIndicator(StorageLocationService locations) {
        this.locations = locations;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        int active = 0;
        int accessible = 0;
        long usableTotal = 0;
        for (StorageLocation loc : locations.all()) {
            if (loc.getStatus() == StorageLocation.Status.DISABLED) {
                continue;
            }
            active++;
            Path root;
            try {
                root = Paths.get(loc.getRootPath());
            } catch (RuntimeException e) {
                builder.withDetail(loc.getName(), "invalid path: " + loc.getRootPath());
                continue;
            }
            boolean ok = Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root);
            if (ok) {
                accessible++;
                usableTotal += loc.getUsableCapacityBytes();
                builder.withDetail(loc.getName(), "ONLINE (" + loc.getRootPath() + ")");
            } else {
                builder.down();
                builder.withDetail(loc.getName(), "OFFLINE (" + loc.getRootPath()
                        + (Files.exists(root) ? " — not a writable directory" : " — path missing") + ")");
            }
        }
        builder.withDetail("locations", active);
        builder.withDetail("accessible", accessible);
        builder.withDetail("usableCapacityBytes", usableTotal);
        if (active == 0) {
            builder.down().withDetail("reason", "no storage locations registered");
        }
        return builder.build();
    }
}

package com.cloudvault.service;

import com.cloudvault.domain.StorageLocation;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.repository.StorageLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** Runs real read/write/capacity health checks against every location. */
@Component
public class StorageHealthJob {

    private static final Logger log = LoggerFactory.getLogger(StorageHealthJob.class);

    private final StorageLocationService locationService;
    private final StorageLocationRepository locations;
    private final BackgroundJobRepository jobs;

    public StorageHealthJob(StorageLocationService locationService,
                            StorageLocationRepository locations,
                            BackgroundJobRepository jobs) {
        this.locationService = locationService;
        this.locations = locations;
        this.jobs = jobs;
    }

    public void run(long jobId) {
        List<StorageLocation> all = locations.findAllOrdered();
        int index = 0;
        for (StorageLocation loc : all) {
            locationService.healthCheck(loc.getId());
            index++;
            final int done = index;
            final int total = all.size();
            jobs.findById(jobId).ifPresent(j -> {
                j.setProgressProcessed(done);
                j.setProgressTotal(total);
                jobs.save(j);
            });
            log.debug("Health check completed for {}", loc.getName());
        }
    }
}

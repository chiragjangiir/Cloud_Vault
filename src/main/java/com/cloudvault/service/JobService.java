package com.cloudvault.service;

import com.cloudvault.domain.BackgroundJob;
import com.cloudvault.domain.User;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Database-backed background job queue. Jobs transition through real states
 * (QUEUED → RUNNING → COMPLETED/FAILED, or CANCELLED) with progress derived
 * from actual work done.
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    public static final String TRASH_CLEANUP = "TRASH_CLEANUP";
    public static final String HEALTH_CHECK = "STORAGE_HEALTH_CHECK";
    public static final String MIGRATION = "STORAGE_MIGRATION";
    public static final String STORAGE_AUDIT = "STORAGE_AUDIT";
    public static final String CHECKSUM_VERIFY = "CHECKSUM_VERIFY";
    public static final String QUOTA_RECONCILE = "QUOTA_RECONCILE";

    private final BackgroundJobRepository jobs;
    private final TrashCleanupJob trashCleanupJob;
    private final StorageHealthJob healthJob;
    private final MigrationService migrationService;
    private final ReconciliationService reconciliationService;
    private final org.springframework.transaction.support.TransactionTemplate tx;

    public JobService(BackgroundJobRepository jobs, TrashCleanupJob trashCleanupJob,
                      StorageHealthJob healthJob, MigrationService migrationService,
                      ReconciliationService reconciliationService,
                      org.springframework.transaction.PlatformTransactionManager txManager) {
        this.jobs = jobs;
        this.trashCleanupJob = trashCleanupJob;
        this.healthJob = healthJob;
        this.migrationService = migrationService;
        this.reconciliationService = reconciliationService;
        this.tx = new org.springframework.transaction.support.TransactionTemplate(txManager);
    }

    @Transactional
    public BackgroundJob enqueue(String type, Map<String, Object> payload, User creator) {
        BackgroundJob job = new BackgroundJob();
        job.setType(type);
        job.setState(BackgroundJob.State.QUEUED);
        job.setPayload(payload == null ? null : Json.write(payload));
        job.setCreatedBy(creator);
        return jobs.save(job);
    }

    @Transactional(readOnly = true)
    public List<BackgroundJob> recent() {
        return jobs.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public BackgroundJob get(long id) {
        return jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job not found"));
    }

    /** Cancels a job that has not started yet (real state transition). */
    @Transactional
    public void cancel(long id) {
        BackgroundJob job = get(id);
        if (job.getState() != BackgroundJob.State.QUEUED) {
            throw ApiException.conflict("Only queued jobs can be cancelled");
        }
        job.setState(BackgroundJob.State.CANCELLED);
        job.setCompletedAt(Instant.now());
        jobs.save(job);
    }

    /**
     * Poll loop: claims the oldest QUEUED job atomically and runs it outside
     * the claiming transaction.
     */
    @Scheduled(fixedDelayString = "${app.job-poll-interval-ms:2000}")
    public void poll() {
        Long claimed = claimNext();
        if (claimed == null) return;
        BackgroundJob job = jobs.findById(claimed).orElse(null);
        if (job == null) return;
        log.info("Job {} ({}) starting", job.getId(), job.getType());
        try {
            run(job);
            BackgroundJob done = jobs.findById(claimed).orElse(null);
            if (done != null && done.getState() == BackgroundJob.State.RUNNING) {
                done.setState(BackgroundJob.State.COMPLETED);
                done.setCompletedAt(Instant.now());
                done.setProgressProcessed(done.getProgressTotal());
                jobs.save(done);
            }
            log.info("Job {} ({}) completed", claimed, job.getType());
        } catch (Exception e) {
            log.error("Job {} ({}) failed", claimed, job.getType(), e);
            jobs.findById(claimed).ifPresent(failed -> {
                failed.setState(BackgroundJob.State.FAILED);
                failed.setErrorMessage(truncate(e.getMessage()));
                failed.setCompletedAt(Instant.now());
                jobs.save(failed);
            });
        }
    }

    private void run(BackgroundJob job) {
        switch (job.getType()) {
            case TRASH_CLEANUP -> trashCleanupJob.run(job.getId());
            case HEALTH_CHECK -> healthJob.run(job.getId());
            case MIGRATION -> migrationService.run(job.getId());
            case STORAGE_AUDIT -> reconciliationService.runAudit(job.getId());
            case CHECKSUM_VERIFY -> reconciliationService.runChecksumVerify(job.getId());
            case QUOTA_RECONCILE -> reconciliationService.runQuotaReconcile(job.getId());
            default -> throw new IllegalStateException("Unknown job type: " + job.getType());
        }
    }

    /** Atomically claims the oldest queued job inside a transaction. */
    protected Long claimNext() {
        return tx.execute(status -> {
            List<BackgroundJob> queued = jobs.findQueuedForUpdate();
            if (queued.isEmpty()) return null;
            BackgroundJob job = queued.get(0);
            job.setState(BackgroundJob.State.RUNNING);
            job.setStartedAt(Instant.now());
            jobs.save(job);
            return job.getId();
        });
    }

    // Progress helpers used by job implementations.
    @Transactional
    public void progress(long jobId, long processed, long total, long bytesProcessed, long bytesTotal) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setProgressProcessed(processed);
            j.setProgressTotal(total);
            j.setBytesProcessed(bytesProcessed);
            j.setBytesTotal(bytesTotal);
            jobs.save(j);
        });
    }

    @Transactional
    public void setTotals(long jobId, long totalObjects, long totalBytes) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setProgressTotal(totalObjects);
            j.setBytesTotal(totalBytes);
            jobs.save(j);
        });
    }

    @Transactional
    public void attachReport(long jobId, String reportJson) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setPayload(reportJson);
            jobs.save(j);
        });
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}

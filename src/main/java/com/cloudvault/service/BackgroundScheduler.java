package com.cloudvault.service;

import com.cloudvault.domain.BackgroundJob;
import com.cloudvault.domain.PasswordResetToken;
import com.cloudvault.domain.Share;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.repository.PasswordResetTokenRepository;
import com.cloudvault.repository.ShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduled maintenance that drives real work: storage health checks, trash
 * cleanup, expired-share processing (real state change + owner notification),
 * stale reservation release and counter purges.
 */
@Component
public class BackgroundScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackgroundScheduler.class);

    private final JobService jobService;
    private final BackgroundJobRepository jobs;
    private final ShareRepository shares;
    private final NotificationService notifications;
    private final AuditService auditService;
    private final QuotaService quotaService;
    private final RateLimitService rateLimitService;
    private final PasswordResetTokenRepository resetTokens;

    public BackgroundScheduler(JobService jobService, BackgroundJobRepository jobs, ShareRepository shares,
                               NotificationService notifications, AuditService auditService,
                               QuotaService quotaService, RateLimitService rateLimitService,
                               PasswordResetTokenRepository resetTokens) {
        this.jobService = jobService;
        this.jobs = jobs;
        this.shares = shares;
        this.notifications = notifications;
        this.auditService = auditService;
        this.quotaService = quotaService;
        this.rateLimitService = rateLimitService;
        this.resetTokens = resetTokens;
    }

    private boolean activeJobOf(String type) {
        return jobs.countByTypeAndStateIn(type, List.of(BackgroundJob.State.QUEUED, BackgroundJob.State.RUNNING)) > 0;
    }

    /** Periodic real storage health checks (queued as visible jobs). */
    @Scheduled(fixedDelayString = "${app.health-check-interval-ms:60000}", initialDelay = 20000)
    public void scheduleHealthChecks() {
        if (!activeJobOf(JobService.HEALTH_CHECK)) {
            jobService.enqueue(JobService.HEALTH_CHECK, Map.of(), null);
        }
    }

    /** Periodic trash cleanup. */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void scheduleTrashCleanup() {
        if (!activeJobOf(JobService.TRASH_CLEANUP)) {
            jobService.enqueue(JobService.TRASH_CLEANUP, Map.of(), null);
        }
    }

    /** Marks expired shares revoked (real state) and notifies their owners once. */
    @Scheduled(fixedDelay = 600000, initialDelay = 45000)
    public void processExpiredShares() {
        List<Share> expired = shares.findExpiredActive(Instant.now());
        for (Share share : expired) {
            share.setRevokedAt(Instant.now());
            shares.save(share);
            notifications.notify(share.getOwner(), NotificationService.SHARE_EXPIRED,
                    "A share link expired",
                    "Access to \"" + share.getFile().getName() + "\" has expired and the link no longer works.",
                    "/app/shares");
            auditService.record("SHARE_EXPIRED", "SHARE", String.valueOf(share.getId()),
                    "file " + share.getFile().getId());
        }
        if (!expired.isEmpty()) {
            log.info("Processed {} expired shares", expired.size());
        }
    }

    /** Releases stale quota reservations from crashed/abandoned uploads. */
    @Scheduled(fixedDelay = 600000, initialDelay = 30000)
    public void releaseStaleReservations() {
        int released = quotaService.releaseExpired();
        if (released > 0) log.info("Released {} expired quota reservations", released);
    }

    /** Purges old rate-limit windows and expired password reset tokens. */
    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void purgeCounters() {
        int windows = rateLimitService.purgeOldWindows(Duration.ofDays(1));
        int tokens = resetTokens.deleteExpired(Instant.now().minus(Duration.ofDays(1)));
        if (windows > 0 || tokens > 0) {
            log.info("Purged {} rate-limit windows, {} expired reset tokens", windows, tokens);
        }
    }
}

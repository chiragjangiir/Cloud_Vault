package com.cloudvault.service;

import com.cloudvault.domain.QuotaReservation;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.domain.UserUsage;
import com.cloudvault.repository.FileVersionRepository;
import com.cloudvault.repository.QuotaReservationRepository;
import com.cloudvault.repository.UserUsageRepository;
import com.cloudvault.web.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Real quota engine.
 *
 * <p>Concurrency model: every quota mutation runs inside a transaction that
 * first takes a pessimistic row lock on {@code user_usage} for the affected
 * user, then validates {@code used + activeReservations + incoming <= quota}.
 * Reservations are committed atomically with usage increments, so two
 * concurrent uploads racing on a shared remaining quota cannot both win.</p>
 *
 * <p>The authoritative truth is the sum of stored file versions; the usage row
 * is a transactionally-maintained index over it, and the background
 * reconciliation job corrects any drift.</p>
 */
@Service
public class QuotaService {

    private final UserUsageRepository usageRepo;
    private final QuotaReservationRepository reservationRepo;
    private final FileVersionRepository versionRepo;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final int reservationTimeoutMinutes;

    public QuotaService(UserUsageRepository usageRepo,
                        QuotaReservationRepository reservationRepo,
                        FileVersionRepository versionRepo,
                        SubscriptionService subscriptionService,
                        NotificationService notificationService,
                        AuditService auditService,
                        com.cloudvault.config.AppProperties props) {
        this.usageRepo = usageRepo;
        this.reservationRepo = reservationRepo;
        this.versionRepo = versionRepo;
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.reservationTimeoutMinutes = props.getReservationTimeoutMinutes();
    }

    /** Current committed usage (transactionally maintained). */
    @Transactional(readOnly = true)
    public long usedBytes(long userId) {
        return usageRepo.findById(userId).map(UserUsage::getUsedBytes).orElse(0L);
    }

    /** Authoritative usage recomputed from stored versions. */
    @Transactional(readOnly = true)
    public long authoritativeUsage(long userId) {
        return versionRepo.sumSizeByOwner(userId);
    }

    public SubscriptionService.Limits limitsFor(User user) {
        long used = usedBytes(user.getId());
        return subscriptionService.limitsFor(user, used);
    }

    /**
     * Reserves quota before any bytes are streamed. Throws 413 when the file
     * exceeds the plan's per-file limit and 507 when the remaining quota is
     * insufficient.
     */
    @Transactional
    public QuotaReservation reserve(User user, long bytes, String reason) {
        if (bytes < 0) throw ApiException.unprocessable("Invalid size");
        SubscriptionService.Limits limits = limitsFor(user);
        if (bytes > limits.maxFileBytes()) {
            throw ApiException.tooLarge("File exceeds the maximum size of " + limits.maxFileBytes() + " bytes for the " + limits.planCode() + " plan");
        }
        usageRepo.ensureRow(user.getId());
        UserUsage usage = usageRepo.findByIdForUpdate(user.getId())
                .orElseThrow(() -> ApiException.conflict("Usage record missing for user"));
        long reserved = reservationRepo.sumActiveReservations(user.getId(), Instant.now());
        if (usage.getUsedBytes() + reserved + bytes > limits.quotaBytes()) {
            notificationService.notify(user, NotificationService.QUOTA_EXCEEDED,
                    "Upload rejected: quota exceeded",
                    "Requested " + bytes + " bytes would exceed your " + limits.planCode() + " plan quota.",
                    "/app/subscription");
            throw ApiException.quotaExceeded("Quota exceeded: you have used "
                    + usage.getUsedBytes() + " of " + limits.quotaBytes() + " bytes");
        }
        QuotaReservation res = new QuotaReservation();
        res.setUser(user);
        res.setBytes(bytes);
        res.setReason(reason);
        res.setExpiresAt(Instant.now().plus(Duration.ofMinutes(reservationTimeoutMinutes)));
        return reservationRepo.save(res);
    }

    /**
     * Commits the reservation together with the actual stored byte count.
     * The caller must already have written and verified the physical object;
     * if this throws, the caller must delete the object and release nothing
     * (the transaction rolls the reservation back to RESERVED only if it was
     * not committed — see {@link #release}). Re-validates against quota for
     * the case where actual bytes differ from the reserved estimate.
     */
    @Transactional
    public void commit(QuotaReservation reservation, long actualBytes) {
        UserUsage usage = usageRepo.findByIdForUpdate(reservation.getUser().getId())
                .orElseThrow(() -> ApiException.conflict("Usage record missing for user"));
        SubscriptionService.Limits limits = subscriptionService.limitsFor(reservation.getUser(), usage.getUsedBytes());
        long now = System.currentTimeMillis();
        long reservedOthers = reservationRepo.sumActiveReservations(reservation.getUser().getId(), Instant.now())
                - reservation.getBytes();
        if (reservedOthers < 0) reservedOthers = 0;
        if (usage.getUsedBytes() + reservedOthers + actualBytes > limits.quotaBytes()) {
            throw ApiException.quotaExceeded("Quota exceeded during upload commit");
        }
        usage.setUsedBytes(usage.getUsedBytes() + actualBytes);
        usage.setUpdatedAt(Instant.now());
        usageRepo.save(usage);
        reservation.setBytes(actualBytes);
        reservation.setStatus(QuotaReservation.Status.COMMITTED);
        reservation.setResolvedAt(Instant.now());
        reservationRepo.save(reservation);
    }

    /** Releases a reservation after a failed upload. Safe to call repeatedly. */
    @Transactional
    public void release(Long reservationId) {
        reservationRepo.findById(reservationId).ifPresent(r -> {
            if (r.getStatus() == QuotaReservation.Status.RESERVED) {
                r.setStatus(QuotaReservation.Status.RELEASED);
                r.setResolvedAt(Instant.now());
                reservationRepo.save(r);
            }
        });
    }

    /** Deducts bytes (object deleted / version pruned). */
    @Transactional
    public void deduct(User user, long bytes) {
        if (bytes <= 0) return;
        usageRepo.ensureRow(user.getId());
        UserUsage usage = usageRepo.findByIdForUpdate(user.getId())
                .orElseThrow(() -> ApiException.conflict("Usage record missing for user"));
        usage.setUsedBytes(Math.max(0, usage.getUsedBytes() - bytes));
        usage.setUpdatedAt(Instant.now());
        usageRepo.save(usage);
    }

    /**
     * Recomputes usage from stored versions and fixes drift. Returns the
     * drift correction applied (non-zero means a real inconsistency existed).
     */
    @Transactional
    public long reconcile(long userId) {
        long authoritative = versionRepo.sumSizeByOwner(userId);
        usageRepo.ensureRow(userId);
        UserUsage usage = usageRepo.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.conflict("Usage record missing for user"));
        long drift = authoritative - usage.getUsedBytes();
        if (drift != 0) {
            usage.setUsedBytes(authoritative);
            usage.setUpdatedAt(Instant.now());
            usageRepo.save(usage);
            auditService.record("QUOTA_RECONCILED", "USER", String.valueOf(userId),
                    "usage drift corrected by " + drift + " bytes");
        }
        return drift;
    }

    /** Releases expired reservations (background job). */
    @Transactional
    public int releaseExpired() {
        var expired = reservationRepo.findExpired(Instant.now());
        Instant now = Instant.now();
        for (QuotaReservation r : expired) {
            r.setStatus(QuotaReservation.Status.RELEASED);
            r.setResolvedAt(now);
        }
        reservationRepo.saveAll(expired);
        return expired.size();
    }
}

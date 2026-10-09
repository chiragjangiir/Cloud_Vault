package com.cloudvault.service;

import com.cloudvault.domain.Plan;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.repository.PlanRepository;
import com.cloudvault.repository.SubscriptionRepository;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the user's effective limits from the database. Backend
 * authorization always goes through this service — the frontend never decides
 * what a plan permits.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    public static final String FREE = "FREE";

    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;

    public SubscriptionService(SubscriptionRepository subscriptions, PlanRepository plans) {
        this.subscriptions = subscriptions;
        this.plans = plans;
    }

    /** Fully-resolved, server-enforced limits for one user. */
    public record Limits(
            long quotaBytes,
            long maxFileBytes,
            boolean sharingEnabled,
            boolean versioningEnabled,
            int maxVersions,
            boolean apiAccess,
            int retentionDays,
            Integer maxDownloadsPerDay,
            String planCode,
            String planName,
            Subscription.Status subscriptionStatus,
            boolean entitled,
            boolean overQuota,
            long usedBytes) {}

    public Plan freePlan() {
        return plans.findByCode(FREE)
                .orElseThrow(() -> new IllegalStateException("FREE plan missing — run database migrations"));
    }

    @Transactional(readOnly = true)
    public Limits limitsFor(User user, long usedBytes) {
        Optional<Subscription> subOpt = subscriptions.findByUserId(user.getId());
        Plan plan;
        Subscription.Status status = null;
        boolean entitled = false;
        Long override = null;
        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            plan = sub.getPlan();
            status = sub.getStatus();
            entitled = sub.isEntitled();
            override = sub.getStorageBytesOverride();
            if (!entitled) {
                // Not entitled right now: fall back to free-tier entitlements.
                plan = freePlan();
                override = null;
            }
        } else {
            plan = freePlan();
            entitled = true; // implicit free tier
            status = Subscription.Status.ACTIVE;
        }
        long quota = override != null ? override : plan.getStorageBytes();
        return new Limits(quota, plan.getMaxFileBytes(), plan.isSharingEnabled(),
                plan.isVersioningEnabled(), plan.getMaxVersions(), plan.isApiAccess(),
                plan.getRetentionDays(), plan.getMaxDownloadsPerDay(), plan.getCode(),
                plan.getName(), status, entitled, usedBytes > quota, usedBytes);
    }

    @Transactional
    public Subscription assignPlan(User user, String planCode, Subscription.Status status, Long storageOverride, String note) {
        Plan plan = plans.findByCode(planCode)
                .orElseThrow(() -> ApiException.unprocessable("Unknown plan: " + planCode));
        Subscription sub = subscriptions.findByUserId(user.getId()).orElseGet(() -> {
            Subscription s = new Subscription();
            s.setUser(user);
            return s;
        });
        sub.setPlan(plan);
        sub.setStatus(status != null ? status : Subscription.Status.ACTIVE);
        sub.setStorageBytesOverride(storageOverride);
        sub.setAdminNote(note);
        sub.setCurrentPeriodStart(Instant.now());
        sub.setCurrentPeriodEnd(null);
        return subscriptions.save(sub);
    }

    public List<Plan> allPlans() {
        return plans.findAllByOrderBySortOrderAsc();
    }

    public Optional<Subscription> subscriptionFor(long userId) {
        return subscriptions.findByUserId(userId);
    }

    /**
     * Server-side guard: throws 403 unless the user's subscription currently
     * allows the feature.
     */
    @Transactional(readOnly = true)
    public void requireSharing(User user) {
        Limits limits = limitsFor(user, 0);
        if (!limits.sharingEnabled()) {
            throw ApiException.forbidden("Your plan does not allow sharing");
        }
    }

    @Transactional(readOnly = true)
    public void requireApiAccess(User user) {
        Limits limits = limitsFor(user, 0);
        if (!limits.apiAccess()) {
            throw ApiException.forbidden("Your plan does not include API access");
        }
    }

    @Transactional
    public void requireNotSuspended(User user) {
        if (user.getStatus() == User.Status.DISABLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Account is disabled");
        }
    }
}

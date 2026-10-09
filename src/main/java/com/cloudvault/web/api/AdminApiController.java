package com.cloudvault.web.api;

import com.cloudvault.domain.BackgroundJob;
import com.cloudvault.domain.Plan;
import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.repository.PlanRepository;
import com.cloudvault.repository.SubscriptionRepository;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.repository.UserSessionRepository;
import com.cloudvault.service.AnalyticsService;
import com.cloudvault.service.AuditService;
import com.cloudvault.service.AuthenticationService;
import com.cloudvault.service.JobService;
import com.cloudvault.service.MigrationService;
import com.cloudvault.service.StorageLocationService;
import com.cloudvault.service.StoragePoolService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Administrator REST API: real users, subscriptions, storage locations,
 * pools, migrations, audits, jobs and audit log — all backed by live
 * database and filesystem operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    public record UserRow(long id, String username, String email, String role, String status,
                          String plan, String subscriptionStatus, long usedBytes, long quotaBytes,
                          Instant createdAt) {}

    public record UserUpdate(String role, String status) {}

    public record PlanAssign(@NotBlank String planCode, String status, Long storageOverride,
                             @Size(max = 500) String note) {}

    public record PasswordReset(@NotBlank @Size(min = 10, max = 200) String newPassword) {}

    public record LocationCreate(@NotBlank String name, @NotBlank String path, Long minFreeBytes) {}

    public record PlanUpdate(Long storageBytes, Long maxFileBytes, Boolean sharingEnabled,
                             Boolean versioningEnabled, Integer maxVersions, Boolean apiAccess,
                             Integer retentionDays) {}

    public record MigrationRequest(long sourceId, long destinationId) {}

    public record VerifyRequest(Long locationId, Integer limit) {}

    private final AnalyticsService analytics;
    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final StorageLocationService locationService;
    private final StoragePoolService poolService;
    private final MigrationService migrationService;
    private final JobService jobService;
    private final AuditService auditService;
    private final AuthenticationService authenticationService;
    private final UserSessionRepository sessionRepo;
    private final SessionRegistry sessionRegistry;
    private final CurrentUser currentUser;
    private final com.cloudvault.service.QuotaService quotaService;

    public AdminApiController(AnalyticsService analytics, UserRepository users,
                              SubscriptionRepository subscriptions, PlanRepository plans,
                              StorageLocationService locationService,
                              StoragePoolService poolService, MigrationService migrationService,
                              JobService jobService, AuditService auditService,
                              AuthenticationService authenticationService, UserSessionRepository sessionRepo,
                              SessionRegistry sessionRegistry, CurrentUser currentUser,
                              com.cloudvault.service.QuotaService quotaService) {
        this.analytics = analytics;
        this.users = users;
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.locationService = locationService;
        this.poolService = poolService;
        this.migrationService = migrationService;
        this.jobService = jobService;
        this.auditService = auditService;
        this.authenticationService = authenticationService;
        this.sessionRepo = sessionRepo;
        this.sessionRegistry = sessionRegistry;
        this.currentUser = currentUser;
        this.quotaService = quotaService;
    }

    private void requireAdmin() {
        currentUser.requireAdmin();
    }

    // ----------------------------- Overview ---------------------------

    @GetMapping("/overview")
    public AnalyticsService.AdminOverview overview() {
        requireAdmin();
        return analytics.adminOverview();
    }

    // ----------------------------- Users ------------------------------

    @GetMapping("/users")
    public List<UserRow> listUsers() {
        requireAdmin();
        return users.findAllLatest().stream().map(this::userRow).toList();
    }

    private UserRow userRow(User u) {
        var sub = subscriptions.findByUserId(u.getId()).orElse(null);
        long quota = 0;
        String plan = "FREE";
        String subStatus = "NONE";
        if (sub != null) {
            plan = sub.getPlan().getCode();
            subStatus = sub.getStatus().name();
            quota = sub.getStorageBytesOverride() != null ? sub.getStorageBytesOverride()
                    : sub.getPlan().getStorageBytes();
        }
        long used = quotaService.usedBytes(u.getId());
        return new UserRow(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(),
                u.getStatus().name(), plan, subStatus, used, quota, u.getCreatedAt());
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable long id, @RequestBody UserUpdate update) {
        requireAdmin();
        User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (update.role() != null && !update.role().isBlank()) {
            try {
                target.setRole(User.Role.valueOf(update.role()));
            } catch (IllegalArgumentException e) {
                throw ApiException.unprocessable("role must be USER or ADMIN");
            }
        }
        if (update.status() != null && !update.status().isBlank()) {
            try {
                target.setStatus(User.Status.valueOf(update.status()));
            } catch (IllegalArgumentException e) {
                throw ApiException.unprocessable("status must be ACTIVE or DISABLED");
            }
            if (target.getStatus() == User.Status.DISABLED) {
                // Revoke every live session for the disabled account.
                for (var s : sessionRepo.findByUserIdOrderByCreatedAtDesc(target.getId())) {
                    SessionInformation info = sessionRegistry.getSessionInformation(s.getId());
                    if (info != null) info.expireNow();
                    sessionRepo.delete(s);
                }
            }
        }
        users.save(target);
        auditService.record("USER_UPDATED", "USER", String.valueOf(id),
                "role=" + target.getRole() + " status=" + target.getStatus());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/plan")
    public ResponseEntity<Void> assignPlan(@PathVariable long id, @Valid @RequestBody PlanAssign req) {
        requireAdmin();
        User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        Subscription.Status status;
        try {
            status = req.status() == null ? Subscription.Status.ACTIVE : Subscription.Status.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessable("Invalid subscription status");
        }
        subscriptionServiceAssign(target, req.planCode(), status, req.storageOverride(), req.note());
        auditService.record("PLAN_ASSIGNED", "USER", String.valueOf(id),
                req.planCode() + "/" + status + (req.storageOverride() != null ? " override=" + req.storageOverride() : ""));
        return ResponseEntity.noContent().build();
    }

    private void subscriptionServiceAssign(User target, String planCode, Subscription.Status status,
                                           Long override, String note) {
        // Direct repository-backed assignment keeps this controller dependency-light.
        Plan plan = plans.findByCode(planCode)
                .orElseThrow(() -> ApiException.unprocessable("Unknown plan: " + planCode));
        Subscription sub = subscriptions.findByUserId(target.getId()).orElseGet(() -> {
            Subscription s = new Subscription();
            s.setUser(target);
            return s;
        });
        sub.setPlan(plan);
        sub.setStatus(status);
        sub.setStorageBytesOverride(override);
        sub.setAdminNote(note);
        sub.setCurrentPeriodStart(Instant.now());
        subscriptions.save(sub);
    }

    @PostMapping("/users/{id}/password-reset")
    public ResponseEntity<Void> resetPassword(@PathVariable long id, @Valid @RequestBody PasswordReset req) {
        User admin = currentUser.requireAdmin();
        User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        authenticationService.adminResetPassword(target, req.newPassword());
        for (var s : sessionRepo.findByUserIdOrderByCreatedAtDesc(target.getId())) {
            SessionInformation info = sessionRegistry.getSessionInformation(s.getId());
            if (info != null) info.expireNow();
            sessionRepo.delete(s);
        }
        auditService.record("PASSWORD_RESET_BY_ADMIN_TRIGGER", "USER", String.valueOf(id),
                "by " + admin.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/sessions")
    public ResponseEntity<Void> revokeUserSessions(@PathVariable long id) {
        requireAdmin();
        User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        int count = 0;
        for (var s : sessionRepo.findByUserIdOrderByCreatedAtDesc(target.getId())) {
            SessionInformation info = sessionRegistry.getSessionInformation(s.getId());
            if (info != null) info.expireNow();
            sessionRepo.delete(s);
            count++;
        }
        auditService.record("SESSIONS_REVOKED", "USER", String.valueOf(id), count + " sessions revoked");
        return ResponseEntity.noContent().build();
    }

    // ----------------------------- Plans ------------------------------

    @GetMapping("/plans")
    public List<Plan> listPlans() {
        requireAdmin();
        return plans.findAllByOrderBySortOrderAsc();
    }

    @PutMapping("/plans/{id}")
    public Plan updatePlan(@PathVariable long id, @RequestBody PlanUpdate req) {
        requireAdmin();
        Plan plan = plans.findById(id).orElseThrow(() -> ApiException.notFound("Plan not found"));
        if (req.storageBytes() != null) {
            if (req.storageBytes() < 0) throw ApiException.unprocessable("storageBytes must be >= 0");
            plan.setStorageBytes(req.storageBytes());
        }
        if (req.maxFileBytes() != null) {
            if (req.maxFileBytes() <= 0) throw ApiException.unprocessable("maxFileBytes must be > 0");
            plan.setMaxFileBytes(req.maxFileBytes());
        }
        if (req.sharingEnabled() != null) plan.setSharingEnabled(req.sharingEnabled());
        if (req.versioningEnabled() != null) plan.setVersioningEnabled(req.versioningEnabled());
        if (req.maxVersions() != null) {
            if (req.maxVersions() < 1) throw ApiException.unprocessable("maxVersions must be >= 1");
            plan.setMaxVersions(req.maxVersions());
        }
        if (req.apiAccess() != null) plan.setApiAccess(req.apiAccess());
        if (req.retentionDays() != null) {
            if (req.retentionDays() < 0) throw ApiException.unprocessable("retentionDays must be >= 0");
            plan.setRetentionDays(req.retentionDays());
        }
        Plan saved = plans.save(plan);
        auditService.record("PLAN_UPDATED", "PLAN", String.valueOf(id),
                "storage=" + saved.getStorageBytes() + " maxFile=" + saved.getMaxFileBytes()
                        + " sharing=" + saved.isSharingEnabled() + " api=" + saved.isApiAccess());
        return saved;
    }

    @GetMapping("/subscriptions")
    public List<Map<String, Object>> listSubscriptions() {
        requireAdmin();
        return subscriptions.findAllWithUserAndPlan().stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "username", s.getUser().getUsername(),
                "plan", s.getPlan().getCode(),
                "status", s.getStatus().name(),
                "storageOverride", s.getStorageBytesOverride() == null ? "null" : s.getStorageBytesOverride().toString()
        )).toList();
    }

    // ----------------------------- Storage ----------------------------

    @GetMapping("/storage/locations")
    public List<Map<String, Object>> listLocations() {
        requireAdmin();
        return locationService.allWithUsage().stream().map(v -> {
            StorageLocation l = v.location();
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("name", l.getName());
            m.put("rootPath", l.getRootPath());
            m.put("provider", l.getProvider());
            m.put("status", l.getStatus().name());
            m.put("filesystem", l.getFilesystem() == null ? "" : l.getFilesystem());
            m.put("totalCapacityBytes", l.getTotalCapacityBytes());
            m.put("usableCapacityBytes", l.getUsableCapacityBytes());
            m.put("usedBytes", v.usedBytes());
            m.put("writable", l.isWritable());
            m.put("lastHealthCheck", l.getLastHealthCheck() == null ? "" : l.getLastHealthCheck().toString());
            m.put("lastHealthMessage", l.getLastHealthMessage() == null ? "" : l.getLastHealthMessage());
            m.put("autoRegistered", l.isAutoRegistered());
            return m;
        }).toList();
    }

    @PostMapping("/storage/locations")
    public ResponseEntity<Map<String, Object>> createLocation(@Valid @RequestBody LocationCreate req) {
        requireAdmin();
        StorageLocation loc = locationService.register(req.name(), req.path(),
                req.minFreeBytes() == null ? 0 : req.minFreeBytes());
        poolService.ensureDefaultPool();
        auditService.record("STORAGE_LOCATION_REGISTERED", "STORAGE_LOCATION", String.valueOf(loc.getId()),
                loc.getName() + " -> " + loc.getRootPath());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", loc.getId(), "status", loc.getStatus().name()));
    }

    @PostMapping("/storage/locations/{id}/health")
    public Map<String, Object> healthCheck(@PathVariable long id) {
        requireAdmin();
        StorageLocation loc = locationService.healthCheck(id);
        auditService.record("STORAGE_HEALTH_CHECK", "STORAGE_LOCATION", String.valueOf(id),
                loc.getName() + " -> " + loc.getStatus());
        return Map.of("id", loc.getId(), "status", loc.getStatus().name(),
                "message", loc.getLastHealthMessage() == null ? "" : loc.getLastHealthMessage());
    }

    @PostMapping("/storage/locations/{id}/disable")
    public ResponseEntity<Void> disableLocation(@PathVariable long id) {
        requireAdmin();
        locationService.disable(id);
        auditService.record("STORAGE_LOCATION_DISABLED", "STORAGE_LOCATION", String.valueOf(id), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/storage/pools")
    public List<StoragePoolService.PoolView> pools() {
        requireAdmin();
        return poolService.allPools();
    }

    @GetMapping("/storage/migration/preview")
    public MigrationService.MigrationPreview migrationPreview(@RequestParam long sourceId,
                                                              @RequestParam long destinationId) {
        requireAdmin();
        return migrationService.preview(sourceId, destinationId);
    }

    @PostMapping("/storage/migrations")
    public ResponseEntity<Map<String, Object>> startMigration(@RequestBody MigrationRequest req) {
        requireAdmin();
        // Validate before queueing so bad requests fail fast.
        MigrationService.MigrationPreview preview = migrationService.preview(req.sourceId(), req.destinationId());
        if (!preview.destinationHasCapacity()) {
            throw ApiException.quotaExceeded("Destination does not have enough free space for this migration");
        }
        BackgroundJob job = jobService.enqueue(JobService.MIGRATION,
                Map.of("sourceId", req.sourceId(), "destinationId", req.destinationId()),
                currentUser.require());
        auditService.record("STORAGE_MIGRATION_STARTED", "JOB", String.valueOf(job.getId()),
                req.sourceId() + " -> " + req.destinationId() + " (" + preview.totalObjects() + " objects)");
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", job.getId()));
    }

    @PostMapping("/storage/audit")
    public ResponseEntity<Map<String, Object>> startAudit() {
        requireAdmin();
        BackgroundJob job = jobService.enqueue(JobService.STORAGE_AUDIT, Map.of(), currentUser.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", job.getId()));
    }

    @PostMapping("/storage/verify")
    public ResponseEntity<Map<String, Object>> startVerify(@RequestBody(required = false) VerifyRequest req) {
        requireAdmin();
        int limit = req == null || req.limit() == null ? 1000 : Math.min(100000, Math.max(1, req.limit()));
        BackgroundJob job = jobService.enqueue(JobService.CHECKSUM_VERIFY,
                Map.of("limit", limit, "locationId", req == null || req.locationId() == null ? 0 : req.locationId()),
                currentUser.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", job.getId()));
    }

    @PostMapping("/storage/quota-reconcile")
    public ResponseEntity<Map<String, Object>> startReconcile() {
        requireAdmin();
        BackgroundJob job = jobService.enqueue(JobService.QUOTA_RECONCILE, Map.of(), currentUser.require());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", job.getId()));
    }

    // ----------------------------- Jobs -------------------------------

    @GetMapping("/jobs")
    public List<BackgroundJob> jobs() {
        requireAdmin();
        return jobService.recent();
    }

    @GetMapping("/jobs/{id}")
    public BackgroundJob job(@PathVariable long id) {
        requireAdmin();
        return jobService.get(id);
    }

    @PostMapping("/jobs/{id}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable long id) {
        requireAdmin();
        jobService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------- Audit log --------------------------

    @GetMapping("/audit")
    public Page<com.cloudvault.domain.AuditLog> audit(@RequestParam(required = false) String action,
                                                      @RequestParam(required = false) String actor,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        requireAdmin();
        return auditService.search(action, actor,
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size))));
    }
}

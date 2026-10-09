package com.cloudvault.web.ui;

import com.cloudvault.domain.Plan;
import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.service.AnalyticsService;
import com.cloudvault.service.AuditService;
import com.cloudvault.service.AuthenticationService;
import com.cloudvault.service.JobService;
import com.cloudvault.service.MigrationService;
import com.cloudvault.service.StorageLocationService;
import com.cloudvault.service.StoragePoolService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.error.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** Administrator pages backed by real database and filesystem operations. */
@Controller
@RequestMapping("/admin")
public class AdminPagesController {

    private final CurrentUser currentUser;
    private final AnalyticsService analytics;
    private final com.cloudvault.repository.UserRepository users;
    private final com.cloudvault.repository.PlanRepository plans;
    private final com.cloudvault.repository.SubscriptionRepository subscriptions;
    private final StorageLocationService locationService;
    private final StoragePoolService poolService;
    private final MigrationService migrationService;
    private final JobService jobService;
    private final AuditService auditService;
    private final AuthenticationService authenticationService;
    private final com.cloudvault.service.QuotaService quotaService;
    private final com.cloudvault.repository.UserSessionRepository sessionRepo;
    private final org.springframework.security.core.session.SessionRegistry sessionRegistry;

    public AdminPagesController(CurrentUser currentUser, AnalyticsService analytics,
                                com.cloudvault.repository.UserRepository users,
                                com.cloudvault.repository.PlanRepository plans,
                                com.cloudvault.repository.SubscriptionRepository subscriptions,
                                StorageLocationService locationService, StoragePoolService poolService,
                                MigrationService migrationService, JobService jobService,
                                AuditService auditService, AuthenticationService authenticationService,
                                com.cloudvault.service.QuotaService quotaService,
                                com.cloudvault.repository.UserSessionRepository sessionRepo,
                                org.springframework.security.core.session.SessionRegistry sessionRegistry) {
        this.currentUser = currentUser;
        this.analytics = analytics;
        this.users = users;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.locationService = locationService;
        this.poolService = poolService;
        this.migrationService = migrationService;
        this.jobService = jobService;
        this.auditService = auditService;
        this.authenticationService = authenticationService;
        this.quotaService = quotaService;
        this.sessionRepo = sessionRepo;
        this.sessionRegistry = sessionRegistry;
    }

    private void revokeSessions(long userId) {
        for (var s : sessionRepo.findByUserIdOrderByCreatedAtDesc(userId)) {
            var info = sessionRegistry.getSessionInformation(s.getId());
            if (info != null) info.expireNow();
            sessionRepo.delete(s);
        }
    }

    private void common(Model model) {
        User admin = currentUser.requireAdmin();
        model.addAttribute("me", admin);
        model.addAttribute("unread", 0L);
        model.addAttribute("isAdmin", true);
        model.addAttribute("fmt", new com.cloudvault.web.UiAdvice.FormatAdapter());
    }

    @GetMapping
    public String dashboard(Model model) {
        common(model);
        model.addAttribute("overview", analytics.adminOverview());
        return "admin/dashboard";
    }

    // ------------------------------ Users -----------------------------

    @GetMapping("/users")
    public String usersPage(Model model) {
        common(model);
        model.addAttribute("users", users.findAllLatest().stream().map(u -> {
            var sub = subscriptions.findByUserId(u.getId()).orElse(null);
            long quota = 0;
            long used = quotaService.usedBytes(u.getId());
            String plan = "FREE";
            String subStatus = "NONE";
            if (sub != null) {
                plan = sub.getPlan().getCode();
                subStatus = sub.getStatus().name();
                quota = sub.getStorageBytesOverride() != null ? sub.getStorageBytesOverride()
                        : sub.getPlan().getStorageBytes();
            }
            return new Object[]{u, plan, subStatus, used, quota};
        }).toList());
        model.addAttribute("plans", plans.findAllByOrderBySortOrderAsc());
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("statuses", User.Status.values());
        model.addAttribute("subStatuses", Subscription.Status.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String setRole(@PathVariable long id, @RequestParam String role, RedirectAttributes ra) {
        currentUser.requireAdmin();
        try {
            User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
            target.setRole(User.Role.valueOf(role));
            users.save(target);
            auditService.record("USER_UPDATED", "USER", String.valueOf(id), "role=" + role);
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/status")
    public String setStatus(@PathVariable long id, @RequestParam String status, RedirectAttributes ra) {
        currentUser.requireAdmin();
        try {
            User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
            target.setStatus(User.Status.valueOf(status));
            users.save(target);
            if (target.getStatus() == User.Status.DISABLED) {
                revokeSessions(target.getId());
            }
            auditService.record("USER_UPDATED", "USER", String.valueOf(id), "status=" + status);
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/plan")
    public String assignPlan(@PathVariable long id, @RequestParam String planCode,
                             @RequestParam String subStatus,
                             @RequestParam(required = false) Long storageOverride,
                             RedirectAttributes ra) {
        currentUser.requireAdmin();
        try {
            User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
            Plan plan = plans.findByCode(planCode)
                    .orElseThrow(() -> ApiException.unprocessable("Unknown plan"));
            Subscription sub = subscriptions.findByUserId(target.getId()).orElseGet(() -> {
                Subscription s = new Subscription();
                s.setUser(target);
                return s;
            });
            sub.setPlan(plan);
            sub.setStatus(Subscription.Status.valueOf(subStatus));
            sub.setStorageBytesOverride(storageOverride);
            subscriptions.save(sub);
            auditService.record("PLAN_ASSIGNED", "USER", String.valueOf(id),
                    planCode + "/" + subStatus + (storageOverride != null ? " override=" + storageOverride : ""));
            ra.addFlashAttribute("message", "Subscription updated for " + target.getUsername());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/password")
    public String resetUserPassword(@PathVariable long id, @RequestParam String newPassword,
                                    RedirectAttributes ra) {
        User admin = currentUser.requireAdmin();
        try {
            User target = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
            authenticationService.adminResetPassword(target, newPassword);
            revokeSessions(target.getId());
            auditService.record("PASSWORD_RESET_BY_ADMIN_TRIGGER", "USER", String.valueOf(id),
                    "by " + admin.getUsername());
            ra.addFlashAttribute("message", "Password reset for " + target.getUsername()
                    + "; their sessions were revoked");
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ------------------------------ Storage ---------------------------

    @GetMapping("/storage")
    public String storagePage(@RequestParam(required = false) Long previewSource,
                              @RequestParam(required = false) Long previewDestination,
                              Model model, RedirectAttributes ra) {
        common(model);
        model.addAttribute("locations", locationService.allWithUsage());
        model.addAttribute("pools", poolService.allPools());
        model.addAttribute("locationStatuses", StorageLocation.Status.values());
        if (previewSource != null && previewDestination != null) {
            try {
                model.addAttribute("preview", migrationService.preview(previewSource, previewDestination));
                model.addAttribute("previewSourceId", previewSource);
                model.addAttribute("previewDestinationId", previewDestination);
            } catch (ApiException e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
        }
        return "admin/storage";
    }

    @PostMapping("/storage/locations")
    public String addLocation(@RequestParam String name, @RequestParam String path,
                              RedirectAttributes ra) {
        currentUser.requireAdmin();
        try {
            StorageLocation loc = locationService.register(name, path, 0);
            poolService.ensureDefaultPool();
            auditService.record("STORAGE_LOCATION_REGISTERED", "STORAGE_LOCATION",
                    String.valueOf(loc.getId()), loc.getName() + " -> " + loc.getRootPath());
            ra.addFlashAttribute("message", "Storage location \"" + loc.getName() + "\" registered and online");
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/storage";
    }

    @PostMapping("/storage/locations/{id}/health")
    public String health(@PathVariable long id, RedirectAttributes ra) {
        currentUser.requireAdmin();
        StorageLocation loc = locationService.healthCheck(id);
        auditService.record("STORAGE_HEALTH_CHECK", "STORAGE_LOCATION", String.valueOf(id),
                loc.getName() + " -> " + loc.getStatus());
        ra.addFlashAttribute("message", loc.getName() + ": " + loc.getStatus()
                + (loc.getLastHealthMessage() != null ? " (" + loc.getLastHealthMessage() + ")" : ""));
        return "redirect:/admin/storage";
    }

    @PostMapping("/storage/migrate")
    public String migrate(@RequestParam long sourceId, @RequestParam long destinationId,
                          RedirectAttributes ra) {
        currentUser.requireAdmin();
        try {
            MigrationService.MigrationPreview preview = migrationService.preview(sourceId, destinationId);
            if (!preview.destinationHasCapacity()) {
                throw ApiException.quotaExceeded("Destination does not have enough free space");
            }
            var job = jobService.enqueue(JobService.MIGRATION,
                    Map.of("sourceId", sourceId, "destinationId", destinationId), currentUser.require());
            auditService.record("STORAGE_MIGRATION_STARTED", "JOB", String.valueOf(job.getId()),
                    sourceId + " -> " + destinationId);
            ra.addFlashAttribute("message", "Migration job #" + job.getId() + " queued — watch progress on the Jobs page");
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/storage";
    }

    @PostMapping("/storage/audit")
    public String startAudit(RedirectAttributes ra) {
        currentUser.requireAdmin();
        var job = jobService.enqueue(JobService.STORAGE_AUDIT, Map.of(), currentUser.require());
        ra.addFlashAttribute("message", "Storage audit job #" + job.getId() + " queued");
        return "redirect:/admin/storage";
    }

    @PostMapping("/storage/verify")
    public String startVerify(RedirectAttributes ra) {
        currentUser.requireAdmin();
        var job = jobService.enqueue(JobService.CHECKSUM_VERIFY, Map.of("limit", 1000, "locationId", 0),
                currentUser.require());
        ra.addFlashAttribute("message", "Checksum verification job #" + job.getId() + " queued");
        return "redirect:/admin/storage";
    }

    @PostMapping("/storage/reconcile")
    public String startReconcile(RedirectAttributes ra) {
        currentUser.requireAdmin();
        var job = jobService.enqueue(JobService.QUOTA_RECONCILE, Map.of(), currentUser.require());
        ra.addFlashAttribute("message", "Quota reconciliation job #" + job.getId() + " queued");
        return "redirect:/admin/storage";
    }

    // ------------------------------ Jobs ------------------------------

    @GetMapping("/jobs")
    public String jobsPage(Model model) {
        common(model);
        model.addAttribute("jobs", jobService.recent());
        return "admin/jobs";
    }

    @PostMapping("/jobs/{id}/cancel")
    public String cancelJob(@PathVariable long id, RedirectAttributes ra) {
        currentUser.requireAdmin();
        try {
            jobService.cancel(id);
            ra.addFlashAttribute("message", "Job #" + id + " cancelled");
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/jobs";
    }

    // ------------------------------ Audit log -------------------------

    @GetMapping("/audit")
    public String auditPage(@RequestParam(required = false) String action,
                            @RequestParam(required = false) String actor,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        common(model);
        var result = auditService.search(action, actor, PageRequest.of(Math.max(0, page), 50));
        model.addAttribute("entries", result.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("action", action == null ? "" : action);
        model.addAttribute("actor", actor == null ? "" : actor);
        return "admin/audit";
    }
}

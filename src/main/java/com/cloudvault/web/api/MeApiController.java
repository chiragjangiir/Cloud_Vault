package com.cloudvault.web.api;

import com.cloudvault.domain.Notification;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.domain.UserSession;
import com.cloudvault.repository.UserSessionRepository;
import com.cloudvault.service.AnalyticsService;
import com.cloudvault.service.ExportService;
import com.cloudvault.service.NotificationService;
import com.cloudvault.service.QuotaService;
import com.cloudvault.service.SubscriptionService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.error.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class MeApiController {

    public record UsageDto(long usedBytes, long quotaBytes, long availableBytes, boolean overQuota,
                           String planCode, String planName, String subscriptionStatus,
                           long maxFileBytes, boolean sharingEnabled, boolean versioningEnabled,
                           int retentionDays) {}

    public record NotificationDto(long id, String type, String title, String body, String link,
                                  Instant createdAt, boolean read) {}

    public record SessionDto(String id, Instant createdAt, Instant lastSeenAt,
                             String ipAddress, String userAgent, boolean current) {}

    private final QuotaService quotaService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final ExportService exportService;
    private final com.cloudvault.service.AuditService auditService;
    private final UserSessionRepository sessionRepo;
    private final SessionRegistry sessionRegistry;
    private final CurrentUser currentUser;

    public MeApiController(QuotaService quotaService, SubscriptionService subscriptionService,
                           NotificationService notificationService, AnalyticsService analyticsService,
                           ExportService exportService, com.cloudvault.service.AuditService auditService,
                           UserSessionRepository sessionRepo,
                           SessionRegistry sessionRegistry, CurrentUser currentUser) {
        this.quotaService = quotaService;
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.exportService = exportService;
        this.auditService = auditService;
        this.sessionRepo = sessionRepo;
        this.sessionRegistry = sessionRegistry;
        this.currentUser = currentUser;
    }

    private static String currentSessionId() {
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
            jakarta.servlet.http.HttpSession session = attrs.getRequest().getSession(false);
            return session == null ? "" : session.getId();
        }
        return "";
    }

    @GetMapping("/usage")
    public UsageDto usage() {
        User user = currentUser.require();
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        return new UsageDto(limits.usedBytes(), limits.quotaBytes(),
                Math.max(0, limits.quotaBytes() - limits.usedBytes()), limits.overQuota(),
                limits.planCode(), limits.planName(),
                limits.subscriptionStatus() == null ? null : limits.subscriptionStatus().name(),
                limits.maxFileBytes(), limits.sharingEnabled(), limits.versioningEnabled(),
                limits.retentionDays());
    }

    @GetMapping("/subscription")
    public Map<String, Object> subscription() {
        User user = currentUser.require();
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        Subscription sub = subscriptionService.subscriptionFor(user.getId()).orElse(null);
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("planCode", limits.planCode());
        out.put("planName", limits.planName());
        out.put("status", limits.subscriptionStatus() == null ? "NONE" : limits.subscriptionStatus().name());
        out.put("quotaBytes", limits.quotaBytes());
        out.put("usedBytes", limits.usedBytes());
        out.put("overQuota", limits.overQuota());
        out.put("sharingEnabled", limits.sharingEnabled());
        out.put("versioningEnabled", limits.versioningEnabled());
        out.put("maxVersions", limits.maxVersions());
        out.put("maxFileBytes", limits.maxFileBytes());
        out.put("retentionDays", limits.retentionDays());
        out.put("startedAt", sub == null ? "" : String.valueOf(sub.getStartedAt()));
        return out;
    }

    @GetMapping("/overview")
    public AnalyticsService.UserOverview overview() {
        return analyticsService.userOverview(currentUser.id());
    }

    // ---------------------------- Notifications -----------------------

    @GetMapping("/notifications")
    public List<NotificationDto> notifications() {
        User user = currentUser.require();
        return notificationService.recentFor(user.getId()).stream()
                .map(n -> new NotificationDto(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                        n.getLink(), n.getCreatedAt(), n.isRead()))
                .toList();
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Void> readAll() {
        notificationService.markAllRead(currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> readOne(@PathVariable long id) {
        notificationService.markRead(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    // ---------------------------- Sessions ----------------------------

    @GetMapping("/sessions")
    public List<SessionDto> sessions() {
        User user = currentUser.require();
        String currentId = currentSessionId();
        return sessionRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(s -> new SessionDto(s.getId(), s.getCreatedAt(), s.getLastSeenAt(),
                        s.getIpAddress(), s.getUserAgent(), s.getId().equals(currentId)))
                .toList();
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@PathVariable String id) {
        User user = currentUser.require();
        UserSession session = sessionRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw ApiException.notFound("Session not found");
        }
        expire(session);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revokeOtherSessions() {
        User user = currentUser.require();
        String currentId = currentSessionId();
        for (UserSession session : sessionRepo.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            if (!session.getId().equals(currentId)) expire(session);
        }
        return ResponseEntity.noContent().build();
    }

    private void expire(UserSession session) {
        SessionInformation info = sessionRegistry.getSessionInformation(session.getId());
        if (info != null) info.expireNow();
        sessionRepo.findById(session.getId()).ifPresent(sessionRepo::delete);
    }

    // ---------------------------- Export ------------------------------

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        User user = currentUser.require();
        String filename = "cloudvault-export-" + user.getUsername() + ".zip";
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename.replace("\"", "_") + "\"");
        exportService.exportUser(user, response.getOutputStream());
        // Audited outside the export's read-only transaction (§58: no silent failures)
        auditService.record("DATA_EXPORTED", "USER", String.valueOf(user.getId()),
                "Data archive downloaded");
        response.flushBuffer();
    }
}

package com.cloudvault.web.ui;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.Folder;
import com.cloudvault.domain.Share;
import com.cloudvault.domain.User;
import com.cloudvault.service.AnalyticsService;
import com.cloudvault.service.FileService;
import com.cloudvault.service.FolderService;
import com.cloudvault.service.NotificationService;
import com.cloudvault.service.ObjectStorageService;
import com.cloudvault.service.QuotaService;
import com.cloudvault.service.RateLimitService;
import com.cloudvault.service.ShareService;
import com.cloudvault.service.SubscriptionService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.RangeStreamer;
import com.cloudvault.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Server-rendered application pages with real form-driven actions (PRG). */
@Controller
@RequestMapping("/app")
public class AppPagesController {

    private final CurrentUser currentUser;
    private final QuotaService quotaService;
    private final SubscriptionService subscriptionService;
    private final FolderService folderService;
    private final FileService fileService;
    private final ShareService shareService;
    private final NotificationService notificationService;
    private final AnalyticsService analytics;
    private final ObjectStorageService objectStorage;
    private final RateLimitService rateLimitService;
    private final com.cloudvault.repository.FileEntryRepository trashRepo;
    private final com.cloudvault.service.AuthenticationService authenticationService;
    private final com.cloudvault.repository.UserSessionRepository sessionRepo;
    private final org.springframework.security.core.session.SessionRegistry sessionRegistry;

    public AppPagesController(CurrentUser currentUser, QuotaService quotaService,
                              SubscriptionService subscriptionService, FolderService folderService,
                              FileService fileService, ShareService shareService,
                              NotificationService notificationService, AnalyticsService analytics,
                              ObjectStorageService objectStorage, RateLimitService rateLimitService,
                              com.cloudvault.repository.FileEntryRepository trashRepo,
                              com.cloudvault.service.AuthenticationService authenticationService,
                              com.cloudvault.repository.UserSessionRepository sessionRepo,
                              org.springframework.security.core.session.SessionRegistry sessionRegistry) {
        this.currentUser = currentUser;
        this.quotaService = quotaService;
        this.subscriptionService = subscriptionService;
        this.folderService = folderService;
        this.fileService = fileService;
        this.shareService = shareService;
        this.notificationService = notificationService;
        this.analytics = analytics;
        this.objectStorage = objectStorage;
        this.rateLimitService = rateLimitService;
        this.trashRepo = trashRepo;
        this.authenticationService = authenticationService;
        this.sessionRepo = sessionRepo;
        this.sessionRegistry = sessionRegistry;
    }

    private void common(Model model, User user) {
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        model.addAttribute("me", user);
        model.addAttribute("limits", limits);
        model.addAttribute("unread", notificationService.unreadCount(user.getId()));
        model.addAttribute("fmt", com.cloudvault.service.Format.class);
        model.addAttribute("isAdmin", user.getRole() == User.Role.ADMIN);
    }

    // ------------------------------ Dashboard -------------------------

    @GetMapping
    public String dashboard(Model model) {
        User user = currentUser.require();
        common(model, user);
        model.addAttribute("overview", analytics.userOverview(user.getId()));
        model.addAttribute("recent", fileService.recent(user));
        model.addAttribute("notifications", notificationService.recentFor(user.getId()));
        return "app/dashboard";
    }

    // ------------------------------ Files -----------------------------

    @GetMapping("/files")
    public String files(@RequestParam(required = false) Long folderId, Model model) {
        User user = currentUser.require();
        common(model, user);
        FolderService.BrowserView view = folderService.browse(user, folderId);
        model.addAttribute("view", view);
        model.addAttribute("allFolders", folderService.allLive(user));
        model.addAttribute("q", "");
        return "app/files";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String ext,
                         @RequestParam(required = false) String mime,
                         @RequestParam(required = false) String from,
                         @RequestParam(required = false) String to,
                         @RequestParam(required = false) Long minSize,
                         @RequestParam(required = false) Long maxSize,
                         Model model) {
        User user = currentUser.require();
        common(model, user);
        FileService.SearchCriteria criteria = new FileService.SearchCriteria(
                q, ext, mime, null, parseInstant(from), parseInstant(to), minSize, maxSize);
        var page = fileService.search(user, criteria,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "updatedAt")));
        model.addAttribute("results", page.getContent());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("ext", ext == null ? "" : ext);
        model.addAttribute("mime", mime == null ? "" : mime);
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        return "app/search";
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.unprocessable("Invalid timestamp");
        }
    }

    // ------------------------------ Upload ----------------------------

    /** Multipart upload with XHR progress on the client (real byte counts). */
    @PostMapping("/upload")
    @ResponseBody
    public java.util.Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam(required = false) Long folderId,
                                                HttpServletRequest request) {
        User user = currentUser.require();
        rateLimitService.consume("upload", String.valueOf(user.getId()), 120, Duration.ofMinutes(1));
        try (InputStream in = file.getInputStream()) {
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            FileEntry stored = fileService.upload(user, folderId, file.getOriginalFilename(),
                    contentType, in, file.getSize());
            return java.util.Map.of("id", stored.getId(), "name", stored.getName(),
                    "size", stored.getSizeBytes());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_READ_FAILED",
                    "Could not read uploaded file: " + e.getMessage());
        }
    }

    @GetMapping("/files/{id}/download")
    public void download(@PathVariable long id, HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        User user = currentUser.require();
        FileService.Download d = fileService.prepareDownload(user, id, null);
        RangeStreamer.write(request, response, d.file().getContentType(), d.file().getName(),
                false, d.version().getSizeBytes(), d.version().getChecksumSha256(),
                () -> objectStorage.openRead(d.object(), 0, -1));
    }

    // ------------------------------ Folders ---------------------------

    @PostMapping("/folders")
    public String createFolder(@RequestParam String name,
                               @RequestParam(required = false) Long parentId,
                               RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> folderService.create(user, parentId, name), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/folders/{id}/rename")
    public String renameFolder(@PathVariable long id, @RequestParam String name,
                               @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> folderService.rename(user, id, name), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/folders/{id}/move")
    public String moveFolder(@PathVariable long id, @RequestParam(required = false) Long targetParentId,
                             @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> folderService.move(user, id, targetParentId), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/folders/{id}/delete")
    public String deleteFolder(@PathVariable long id,
                               @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> folderService.trash(user, id), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/folders/{id}/restore")
    public String restoreFolder(@PathVariable long id, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> folderService.restore(user, id), ra);
        return "redirect:/app/trash";
    }

    // ------------------------------ File actions ----------------------

    @PostMapping("/files/{id}/rename")
    public String renameFile(@PathVariable long id, @RequestParam String name,
                             @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> fileService.rename(user, id, name), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/files/{id}/move")
    public String moveFile(@PathVariable long id, @RequestParam Long targetFolderId,
                           @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> fileService.move(user, id, targetFolderId), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/files/{id}/delete")
    public String deleteFile(@PathVariable long id,
                             @RequestParam(required = false) Long parentId, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> fileService.trash(user, id), ra);
        return backToFolder(parentId);
    }

    @PostMapping("/files/{id}/restore")
    public String restoreFile(@PathVariable long id, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> fileService.restore(user, id), ra);
        return "redirect:/app/trash";
    }

    @PostMapping("/files/{id}/purge")
    public String purgeFile(@PathVariable long id, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> fileService.purge(user, id), ra);
        return "redirect:/app/trash";
    }

    // ------------------------------ Trash -----------------------------

    @GetMapping("/trash")
    public String trash(Model model) {
        User user = currentUser.require();
        common(model, user);
        model.addAttribute("trashedFiles", trashRepo.findTrashedByOwnerOrdered(user.getId()));
        model.addAttribute("trashedFolders", folderService.trashedRoots(user));
        return "app/trash";
    }

    // ------------------------------ Shares ----------------------------

    @GetMapping("/shares")
    public String shares(Model model) {
        User user = currentUser.require();
        common(model, user);
        model.addAttribute("owned", shareService.ownedBy(user));
        model.addAttribute("incoming", shareService.sharedWith(user));
        model.addAttribute("permValues", Share.Permission.values());
        return "app/shares";
    }

    @PostMapping("/shares")
    public String createShare(@RequestParam long fileId,
                              @RequestParam String type,
                              @RequestParam String permission,
                              @RequestParam(required = false) String recipient,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresAt,
                              RedirectAttributes ra) {
        User user = currentUser.require();
        try {
            Share.Type t = Share.Type.valueOf(type);
            Share.Permission p = Share.Permission.valueOf(permission);
            if (t == Share.Type.PUBLIC_LINK) {
                Share share = shareService.createPublicLink(user, fileId, p, expiresAt, null);
                ra.addFlashAttribute("message", "Share link created: " + shareService.publicUrl(share.getToken()));
            } else {
                shareService.createUserShare(user, fileId, recipient, p, expiresAt);
                ra.addFlashAttribute("message", "File shared with " + recipient);
            }
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/shares";
    }

    @PostMapping("/shares/{id}/revoke")
    public String revokeShare(@PathVariable long id, RedirectAttributes ra) {
        User user = currentUser.require();
        run(() -> shareService.revoke(user, id), ra);
        return "redirect:/app/shares";
    }

    // ------------------------------ Subscription / settings ----------

    @GetMapping("/subscription")
    public String subscription(Model model) {
        User user = currentUser.require();
        common(model, user);
        model.addAttribute("plans", subscriptionService.allPlans());
        model.addAttribute("current", subscriptionService.subscriptionFor(user.getId()).orElse(null));
        return "app/subscription";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        User user = currentUser.require();
        common(model, user);
        model.addAttribute("sessions", sessionRepo.findByUserIdOrderByCreatedAtDesc(user.getId()));
        return "app/settings";
    }

    @PostMapping("/settings/sessions/{id}/revoke")
    public String revokeSession(@PathVariable String id, RedirectAttributes ra) {
        User user = currentUser.require();
        var session = sessionRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw ApiException.notFound("Session not found");
        }
        expireSession(session);
        ra.addFlashAttribute("message", "Session revoked");
        return "redirect:/app/settings";
    }

    @PostMapping("/settings/sessions/revoke-others")
    public String revokeOtherSessions(RedirectAttributes ra) {
        User user = currentUser.require();
        String currentId = currentSessionId();
        int count = 0;
        for (var session : sessionRepo.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            if (!session.getId().equals(currentId)) {
                expireSession(session);
                count++;
            }
        }
        ra.addFlashAttribute("message", count + " session(s) signed out");
        return "redirect:/app/settings";
    }

    private static String currentSessionId() {
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
            jakarta.servlet.http.HttpSession s = attrs.getRequest().getSession(false);
            return s == null ? "" : s.getId();
        }
        return "";
    }

    private void expireSession(com.cloudvault.domain.UserSession session) {
        var info = sessionRegistry.getSessionInformation(session.getId());
        if (info != null) info.expireNow();
        sessionRepo.delete(session);
    }

    @PostMapping("/settings/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        User user = currentUser.require();
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/app/settings";
        }
        try {
            authenticationService.changePassword(user, currentPassword, newPassword);
            ra.addFlashAttribute("message", "Password updated");
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/settings";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        User user = currentUser.require();
        common(model, user);
        model.addAttribute("notifications", notificationService.recentFor(user.getId()));
        return "app/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAllNotifications() {
        notificationService.markAllRead(currentUser.id());
        return "redirect:/app/notifications";
    }

    // ------------------------------ Public share page -----------------

    // ------------------------------ Helpers ---------------------------

    private void run(Runnable action, RedirectAttributes ra) {
        try {
            action.run();
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
    }

    private static String backToFolder(Long parentId) {
        return parentId == null ? "redirect:/app/files" : "redirect:/app/files?folderId=" + parentId;
    }
}

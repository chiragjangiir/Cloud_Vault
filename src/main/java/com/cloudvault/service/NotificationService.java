package com.cloudvault.service;

import com.cloudvault.domain.Notification;
import com.cloudvault.domain.User;
import com.cloudvault.repository.NotificationRepository;
import com.cloudvault.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Creates notifications only from real system events (share created/expired,
 * quota exceeded, storage unavailable, migration completed, ...).
 */
@Service
public class NotificationService {

    public static final String SHARE_CREATED = "SHARE_CREATED";
    public static final String SHARE_EXPIRED = "SHARE_EXPIRED";
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    public static final String STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE";
    public static final String MIGRATION_COMPLETED = "MIGRATION_COMPLETED";
    public static final String MIGRATION_FAILED = "MIGRATION_FAILED";
    public static final String TRASH_PURGED = "TRASH_PURGED";
    public static final String PLAN_CHANGED = "PLAN_CHANGED";
    public static final String JOB_FAILED = "JOB_FAILED";

    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationService(NotificationRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    /** Notifies every administrator (used for storage/job events). */
    public void notifyAdmins(String type, String title, String body) {
        users.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN && u.getStatus() == User.Status.ACTIVE)
                .forEach(admin -> notify(admin, type, title, body, "/admin/jobs"));
    }

    public void notify(User user, String type, String title, String body, String link) {
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        repo.save(n);
    }

    public List<Notification> recentFor(long userId) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    public void markAllRead(long userId) {
        List<Notification> unread = repo.findUnread(userId);
        java.time.Instant now = java.time.Instant.now();
        unread.forEach(n -> n.setReadAt(now));
        repo.saveAll(unread);
    }

    public void markRead(long id, long userId) {
        repo.findById(id).ifPresent(n -> {
            if (n.getUser().getId().equals(userId) && n.getReadAt() == null) {
                n.setReadAt(java.time.Instant.now());
                repo.save(n);
            }
        });
    }
}

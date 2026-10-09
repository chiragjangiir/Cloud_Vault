package com.cloudvault.service;

import com.cloudvault.domain.BackgroundJob;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.repository.FileEntryRepository;
import com.cloudvault.repository.ShareRepository;
import com.cloudvault.repository.SubscriptionRepository;
import com.cloudvault.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every dashboard number comes from a real aggregate query — no hardcoded,
 * generated or cached-fake statistics.
 */
@Service
public class AnalyticsService {

    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final FileEntryRepository files;
    private final ShareRepository shares;
    private final BackgroundJobRepository jobs;
    private final StoragePoolService poolService;
    private final StorageObjectUsage objectUsage;
    private final JdbcTemplate jdbc;
    private final QuotaService quotaService;

    public AnalyticsService(UserRepository users, SubscriptionRepository subscriptions,
                            FileEntryRepository files, ShareRepository shares,
                            BackgroundJobRepository jobs, StoragePoolService poolService,
                            StorageObjectUsage objectUsage, JdbcTemplate jdbc, QuotaService quotaService) {
        this.users = users;
        this.subscriptions = subscriptions;
        this.files = files;
        this.shares = shares;
        this.jobs = jobs;
        this.poolService = poolService;
        this.objectUsage = objectUsage;
        this.jdbc = jdbc;
        this.quotaService = quotaService;
    }

    public record CountByLabel(String label, long count, long bytes) {}

    public record AdminOverview(
            long totalUsers,
            long activeUsers,
            Map<String, Long> subscriptionsByStatus,
            long activeFiles,
            long trashedFiles,
            long storedBytes,
            long trashedBytes,
            long activeShares,
            long totalObjects,
            Map<String, Long> jobsByState,
            List<CountByLabel> storageByType,
            List<CountByLabel> uploadsByDay,
            List<StoragePoolService.PoolView> pools) {}

    @Transactional(readOnly = true)
    public AdminOverview adminOverview() {
        Map<String, Long> subs = new LinkedHashMap<>();
        for (Subscription.Status status : Subscription.Status.values()) {
            subs.put(status.name(), subscriptions.countByStatus(status));
        }
        Map<String, Long> jobStates = new LinkedHashMap<>();
        for (BackgroundJob.State state : BackgroundJob.State.values()) {
            jobStates.put(state.name(), jobs.countByState(state));
        }
        long activeFiles = files.countByStatus(com.cloudvault.domain.FileEntry.Status.ACTIVE);
        long trashedFiles = files.countByStatus(com.cloudvault.domain.FileEntry.Status.TRASHED);
        long storedBytes = files.sumActiveSize();

        return new AdminOverview(
                users.count(),
                users.findAll().stream().filter(u -> u.getStatus() == com.cloudvault.domain.User.Status.ACTIVE).count(),
                subs,
                activeFiles,
                trashedFiles,
                storedBytes,
                files.sumTrashedSize(),
                shares.findAll().stream().filter(s -> s.getRevokedAt() == null).count(),
                objectUsage.totalObjectCount(),
                jobStates,
                storageByType(null),
                uploadsByDay(),
                poolService.allPools());
    }

    /** Aggregated storage usage grouped by MIME top-level type (real rows). */
    @Transactional(readOnly = true)
    public List<CountByLabel> storageByType(Long ownerId) {
        String sql = ownerId == null
                ? "SELECT split_part(content_type, '/', 1) AS label, COUNT(*) AS cnt, COALESCE(SUM(size_bytes), 0) AS bytes "
                  + "FROM files WHERE status = 'ACTIVE' GROUP BY 1 ORDER BY 3 DESC"
                : "SELECT split_part(content_type, '/', 1) AS label, COUNT(*) AS cnt, COALESCE(SUM(size_bytes), 0) AS bytes "
                  + "FROM files WHERE status = 'ACTIVE' AND owner_id = ? GROUP BY 1 ORDER BY 3 DESC";
        List<CountByLabel> result = new ArrayList<>();
        jdbc.query(sql, (rs, i) -> result.add(new CountByLabel(rs.getString("label"),
                rs.getLong("cnt"), rs.getLong("bytes"))),
                ownerId == null ? new Object[]{} : new Object[]{ownerId});
        return result;
    }

    /** Real upload events per day derived from the audit log (last 14 days). */
    @Transactional(readOnly = true)
    public List<CountByLabel> uploadsByDay() {
        List<CountByLabel> result = new ArrayList<>();
        jdbc.query("""
                SELECT to_char(date_trunc('day', created_at), 'YYYY-MM-DD') AS label,
                       COUNT(*) AS cnt, 0::bigint AS bytes
                FROM audit_logs
                WHERE action = 'FILE_UPLOADED'
                  AND created_at > now() - interval '14 days'
                GROUP BY 1 ORDER BY 1
                """, (rs, i) -> result.add(new CountByLabel(rs.getString("label"),
                rs.getLong("cnt"), rs.getLong("bytes"))));
        return result;
    }

    public record UserOverview(long usedBytes, long quotaBytes, long activeFiles, long trashedFiles,
                               long shareCount, List<CountByLabel> storageByType) {}

    @Transactional(readOnly = true)
    public UserOverview userOverview(long userId) {
        User user = users.findById(userId).orElseThrow();
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        return new UserOverview(
                limits.usedBytes(),
                limits.quotaBytes(),
                files.countByOwnerId(userId),
                files.countByOwnerIdAndStatus(userId, com.cloudvault.domain.FileEntry.Status.TRASHED),
                shares.countByOwnerIdAndRevokedAtIsNull(userId),
                storageByType(userId));
    }
}

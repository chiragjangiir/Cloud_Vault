package com.cloudvault.service;

import com.cloudvault.domain.StorageLocation;
import com.cloudvault.domain.StorageObject;
import com.cloudvault.domain.User;
import com.cloudvault.repository.BackgroundJobRepository;
import com.cloudvault.repository.StorageLocationRepository;
import com.cloudvault.repository.StorageObjectRepository;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.service.storage.StorageProvider;
import com.cloudvault.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Administrative consistency checks based on REAL scans:
 * database objects vs physical files, orphaned files, missing files, checksum
 * verification and per-user quota reconciliation. Findings are reported —
 * nothing is silently "fixed".
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final int SAMPLE_LIMIT = 100;

    private final StorageLocationRepository locations;
    private final StorageObjectRepository objects;
    private final BackgroundJobRepository jobs;
    private final UserRepository users;
    private final ObjectStorageService objectStorage;
    private final QuotaService quotaService;
    private final AuditService auditService;

    public ReconciliationService(StorageLocationRepository locations, StorageObjectRepository objects,
                                 BackgroundJobRepository jobs, UserRepository users,
                                 ObjectStorageService objectStorage, QuotaService quotaService,
                                 AuditService auditService) {
        this.locations = locations;
        this.objects = objects;
        this.jobs = jobs;
        this.users = users;
        this.objectStorage = objectStorage;
        this.quotaService = quotaService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------
    // Storage audit: DB rows vs files on disk
    // ------------------------------------------------------------------

    public void runAudit(long jobId) {
        List<Map<String, Object>> locationReports = new ArrayList<>();
        long sumDbObjects = 0;
        long sumPhysical = 0;
        long sumMissing = 0;
        long sumOrphans = 0;

        for (StorageLocation loc : locations.findAllOrdered()) {
            Map<String, Object> report = auditLocation(loc);
            locationReports.add(report);
            sumDbObjects += (Long) report.get("dbObjects");
            sumPhysical += (Long) report.get("physicalObjects");
            sumMissing += (Long) report.get("missingOnDisk");
            sumOrphans += (Long) report.get("orphansOnDisk");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("kind", "STORAGE_AUDIT");
        payload.put("locations", locationReports);
        payload.put("summary", Map.of(
                "dbObjects", sumDbObjects,
                "physicalObjects", sumPhysical,
                "missingOnDisk", sumMissing,
                "orphansOnDisk", sumOrphans));
        jobs.findById(jobId).ifPresent(j -> {
            j.setPayload(Json.pretty(payload));
            jobs.save(j);
        });
        auditService.record("STORAGE_AUDIT_COMPLETED", "JOB", String.valueOf(jobId),
                "db=" + sumDbObjects + " physical=" + sumPhysical
                        + " missing=" + sumMissing + " orphans=" + sumOrphans);
        log.info("Storage audit finished: {} db objects, {} physical, {} missing, {} orphans",
                sumDbObjects, sumPhysical, sumMissing, sumOrphans);
    }

    private Map<String, Object> auditLocation(StorageLocation loc) {
        Map<String, Long> physical = scanPhysical(loc);
        List<StorageObject> dbObjects = objects.findByLocationId(loc.getId());
        Map<String, StorageObject> dbByKey = new HashMap<>();
        for (StorageObject o : dbObjects) dbByKey.put(o.getStorageKey(), o);

        List<String> missing = new ArrayList<>();
        List<String> orphans = new ArrayList<>();
        long dbBytes = 0;
        long physicalBytes = physical.values().stream().mapToLong(Long::longValue).sum();

        for (StorageObject o : dbObjects) {
            dbBytes += o.getSizeBytes();
            if (!physical.containsKey(o.getStorageKey())) missing.add(o.getStorageKey());
        }
        for (String key : physical.keySet()) {
            if (!dbByKey.containsKey(key)) orphans.add(key);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("locationId", loc.getId());
        report.put("name", loc.getName());
        report.put("status", loc.getStatus().name());
        report.put("rootPath", loc.getRootPath());
        report.put("dbObjects", (long) dbObjects.size());
        report.put("physicalObjects", (long) physical.size());
        report.put("dbBytes", dbBytes);
        report.put("physicalBytes", physicalBytes);
        report.put("missingOnDisk", (long) missing.size());
        report.put("orphansOnDisk", (long) orphans.size());
        report.put("missingSample", missing.stream().limit(SAMPLE_LIMIT).toList());
        report.put("orphanSample", orphans.stream().limit(SAMPLE_LIMIT).toList());
        return report;
    }

    /** Walks the location root and maps relative storage keys to byte sizes. */
    private Map<String, Long> scanPhysical(StorageLocation loc) {
        Map<String, Long> result = new HashMap<>();
        Path root = Path.of(loc.getRootPath());
        if (!Files.isDirectory(root)) {
            log.warn("Cannot scan {}: not a directory", root);
            return result;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    // Skip transient write temps and health probes at the root.
                    if (name.contains(".part-")) return false;
                    Path rel = root.relativize(p);
                    return rel.getNameCount() > 0 && !rel.getName(0).toString().startsWith(".");
                })
                .forEach(p -> {
                    try {
                        String key = root.relativize(p).toString().replace('\\', '/');
                        result.put(key, Files.size(p));
                    } catch (IOException e) {
                        log.warn("Could not stat {} during audit: {}", p, e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.error("Failed to scan {}: {}", root, e.getMessage());
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Checksum verification
    // ------------------------------------------------------------------

    public void runChecksumVerify(long jobId) {
        var job = jobs.findById(jobId).orElseThrow();
        Map<String, Object> payload = Json.readMap(job.getPayload());
        int limit = payload.get("limit") instanceof Number n ? n.intValue() : 1000;
        Long locationId = payload.get("locationId") instanceof Number n ? n.longValue() : null;

        List<StorageObject> candidates = locationId != null
                ? objects.findByLocationId(locationId)
                : objects.findAll();
        List<Map<String, Object>> mismatches = new ArrayList<>();
        int checked = 0;
        int missing = 0;
        long bytesChecked = 0;

        for (StorageObject obj : candidates) {
            if (checked >= limit) break;
            String actual;
            try {
                actual = objectStorage.checksum(obj);
            } catch (IOException | RuntimeException e) {
                missing++;
                Map<String, Object> m = new HashMap<>();
                m.put("storageKey", obj.getStorageKey());
                m.put("problem", "unreadable: " + e.getMessage());
                mismatches.add(m);
                checked++;
                continue;
            }
            checked++;
            bytesChecked += obj.getSizeBytes();
            if (!actual.equalsIgnoreCase(obj.getChecksumSha256())) {
                Map<String, Object> m = new HashMap<>();
                m.put("storageKey", obj.getStorageKey());
                m.put("expected", obj.getChecksumSha256());
                m.put("actual", actual);
                mismatches.add(m);
            }
            if (checked % 50 == 0) {
                update(jobId, checked, candidates.size(), bytesChecked);
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("kind", "CHECKSUM_VERIFY");
        report.put("checked", checked);
        report.put("unreadable", missing);
        report.put("mismatched", mismatches.size());
        report.put("mismatchSample", mismatches.stream().limit(SAMPLE_LIMIT).toList());
        report.put("totalCandidates", candidates.size());
        finish(jobId, Json.pretty(report), checked, candidates.size());
        auditService.record("CHECKSUM_VERIFY_COMPLETED", "JOB", String.valueOf(jobId),
                "checked=" + checked + " mismatched=" + mismatches.size());
    }

    private void update(long jobId, long processed, long total, long bytesProcessed) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setProgressProcessed(processed);
            j.setProgressTotal(total);
            j.setBytesProcessed(bytesProcessed);
            jobs.save(j);
        });
    }

    private void finish(long jobId, String payloadJson, long processed, long total) {
        jobs.findById(jobId).ifPresent(j -> {
            j.setPayload(payloadJson);
            j.setProgressProcessed(processed);
            j.setProgressTotal(total);
            jobs.save(j);
        });
    }

    // ------------------------------------------------------------------
    // Quota reconciliation
    // ------------------------------------------------------------------

    @Transactional
    public void runQuotaReconcile(long jobId) {
        int corrected = 0;
        long totalDrift = 0;
        int usersChecked = 0;
        for (User user : users.findAll()) {
            long drift = quotaService.reconcile(user.getId());
            usersChecked++;
            if (drift != 0) {
                corrected++;
                totalDrift += Math.abs(drift);
            }
        }
        Map<String, Object> report = new HashMap<>();
        report.put("kind", "QUOTA_RECONCILE");
        report.put("usersChecked", usersChecked);
        report.put("usersCorrected", corrected);
        report.put("totalDriftBytes", totalDrift);
        finish(jobId, Json.pretty(report), usersChecked, usersChecked);
    }
}

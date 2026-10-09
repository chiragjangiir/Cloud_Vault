package com.cloudvault.it;

import com.cloudvault.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin workflows (§23, §25, §29, §33, §41): real dashboard data, plan changes
 * that propagate immediately, a genuine ZIP export of user data, and a real
 * storage migration job (copy → checksum verify → metadata switch → source
 * delete) with progress derived from actual processed-object counts.
 */
class AdminExportMigrationTest extends BaseIntegrationTest {

    @Test
    void adminDashboardsExposeOnlyRealData() throws Exception {
        Client admin = admin();

        admin.get("/api/v1/admin/overview").andExpect(status().isOk());
        admin.get("/api/v1/admin/audit").andExpect(status().isOk());
        admin.get("/api/v1/admin/subscriptions").andExpect(status().isOk());

        // storage locations reflect the real filesystem (§22, §26)
        JsonNode locs = jsonOf(admin.get("/api/v1/admin/storage/locations")
                .andExpect(status().isOk()).andReturn());
        assertTrue(locs.size() >= 1, "the default location must be registered");
        boolean onlineWithCapacity = false;
        for (JsonNode l : locs) {
            if ("ONLINE".equals(l.path("status").asText()) && l.path("totalCapacityBytes").asLong() > 0) {
                onlineWithCapacity = true;
            }
        }
        assertTrue(onlineWithCapacity, "a real ONLINE location with capacity > 0 must exist");

        // users and plans come from the database (§23)
        JsonNode users = jsonOf(admin.get("/api/v1/admin/users")
                .andExpect(status().isOk()).andReturn());
        assertNotNull(findBy(users, "username", ADMIN_USER), "admin account must be listed");
        JsonNode plans = jsonOf(admin.get("/api/v1/admin/plans")
                .andExpect(status().isOk()).andReturn());
        assertTrue(plans.size() >= 2, "seeded plans must be visible");

        // pools carry capacity computed from registered storage (§6)
        JsonNode pools = jsonOf(admin.get("/api/v1/admin/storage/pools")
                .andExpect(status().isOk()).andReturn());
        assertTrue(pools.size() >= 1, "the default pool must exist");
        assertTrue(pools.get(0).path("totalCapacityBytes").asLong() > 0,
                "pool capacity must come from real registered storage");

        // jobs endpoint returns the real job table (§28)
        JsonNode jobs = jsonOf(admin.get("/api/v1/admin/jobs")
                .andExpect(status().isOk()).andReturn());
        assertTrue(jobs.isArray());

        // a normal user is locked out of every admin API (§35)
        Client u = registerAndLogin("nospy");
        u.get("/api/v1/admin/overview").andExpect(status().isForbidden());
        u.get("/api/v1/admin/users").andExpect(status().isForbidden());
    }

    @Test
    void planChangesPersistAndPropagateImmediately() throws Exception {
        Client admin = admin();
        JsonNode plans = jsonOf(admin.get("/api/v1/admin/plans")
                .andExpect(status().isOk()).andReturn());
        JsonNode free = findBy(plans, "code", "FREE");
        assertNotNull(free);
        long planId = free.get("id").asLong();
        long original = free.get("storageBytes").asLong();
        try {
            admin.putJson("/api/v1/admin/plans/" + planId, "{\"storageBytes\":5000000000}")
                    .andExpect(status().isOk());
            JsonNode after = jsonOf(admin.get("/api/v1/admin/plans")
                    .andExpect(status().isOk()).andReturn());
            assertEquals(5000000000L, findBy(after, "code", "FREE").get("storageBytes").asLong(),
                    "plan change must persist in the database");

            // a FREE user sees the new quota from the SERVER, immediately (§10, §31, §33)
            Client user = registerAndLogin("planupd");
            user.get("/api/v1/auth/me").andExpect(status().isOk())
                    .andExpect(jsonPath("$.quotaBytes").value(5000000000L));
        } finally {
            admin.putJson("/api/v1/admin/plans/" + planId, "{\"storageBytes\":" + original + "}")
                    .andExpect(status().isOk());
        }
    }

    @Test
    void exportProducesAGenuineArchiveOfUserData() throws Exception {
        Client u = registerAndLogin("export");
        String folderName = "Docs-" + unique("");
        JsonNode folder = jsonOf(u.postJson("/api/v1/folders",
                "{\"name\":\"" + folderName + "\",\"parentId\":null}")
                .andExpect(status().isCreated()).andReturn());

        byte[] payload = randomBytes(20 * 1024);
        String fileName = "exported-" + unique("") + ".bin";
        upload(u, fileName, payload, folder.get("id").asLong());

        byte[] zipBytes = u.get("/api/v1/me/export")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        boolean manifestSeen = false;
        String manifestJson = null;
        byte[] fileBytes = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName())) {
                    manifestSeen = true;
                    manifestJson = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } else if (entry.getName().endsWith(fileName)) {
                    fileBytes = zis.readAllBytes();
                }
            }
        }
        assertTrue(manifestSeen, "export must contain manifest.json (§41)");
        assertNotNull(manifestJson);
        assertTrue(manifestJson.contains(u.username), "manifest must describe the real account");
        assertTrue(manifestJson.contains(folderName), "manifest must contain the real folder");
        assertNotNull(fileBytes, "export must contain the real file bytes, not a fake entry (§41)");
        assertArrayEquals(payload, fileBytes, "exported bytes must match the uploaded file");
    }

    @Test
    void storageMigrationCopiesVerifiesAndKeepsFilesReadable() throws Exception {
        Client admin = admin();

        // 1. Upload FIRST, while the default location is the only registered
        //    one, so placement is deterministic (§7: pool picks a real location).
        Client u = registerAndLogin("migr");
        byte[] payload = randomBytes(48 * 1024);
        String sha = sha256Hex(payload);
        JsonNode f = upload(u, "migrate-" + unique("") + ".bin", payload, null);
        long fileId = f.get("id").asLong();
        assertFalse(storedWithSha(sha).isEmpty(), "precondition: bytes on the source location");

        // 2. Register a real second location on disk (§5, §25)
        Path dest = Files.createDirectories(
                Paths.get("target", "it-dest-" + unique("d")).toAbsolutePath());
        JsonNode created = jsonOf(admin.postJson("/api/v1/admin/storage/locations",
                        "{\"name\":\"MIGDEST-" + unique("d") + "\",\"path\":\"" + dest + "\"}")
                .andExpect(status().isCreated()).andReturn());
        long destId = created.get("id").asLong();

        JsonNode locs = jsonOf(admin.get("/api/v1/admin/storage/locations")
                .andExpect(status().isOk()).andReturn());
        long srcId = -1;
        for (JsonNode l : locs) {
            if (l.path("rootPath").asText().contains("it-storage")) {
                srcId = l.path("id").asLong();
            }
        }
        assertTrue(srcId > 0, "default storage location must be registered");

        // 3. Preview reports real numbers (§29)
        JsonNode preview = jsonOf(admin.get("/api/v1/admin/storage/migration/preview?sourceId=" + srcId
                        + "&destinationId=" + destId)
                .andExpect(status().isOk()).andReturn());
        assertTrue(preview.get("totalObjects").asLong() >= 1,
                "preview must count the real objects on the source");
        assertTrue(preview.get("destinationHasCapacity").asBoolean(),
                "destination on the same filesystem must have capacity");

        // migrating a location onto itself is rejected (§25 safety)
        admin.get("/api/v1/admin/storage/migration/preview?sourceId=" + srcId + "&destinationId=" + srcId)
                .andExpect(status().isUnprocessableEntity());

        // 4. Start the real job and wait for COMPLETED (§27–§29)
        long before = maxJobId(admin);
        admin.postJson("/api/v1/admin/storage/migrations",
                        "{\"sourceId\":" + srcId + ",\"destinationId\":" + destId + "}")
                .andExpect(status().is2xxSuccessful());
        long jobId = awaitMigrationJob(admin, before);
        JsonNode job = awaitJobState(admin, jobId, "COMPLETED");
        assertTrue(job.get("progressProcessed").asLong() >= 1,
                "migration progress must derive from real processed objects");
        assertTrue(job.get("progressTotal").asLong() >= 1);

        // 5. Data safety (§25): file still downloadable with identical bytes
        assertArrayEquals(payload, download(u, fileId));

        // 6. Physical verification: bytes now at the destination, source removed
        assertFalse(storedWithSha(dest, sha).isEmpty(), "destination must hold the migrated bytes");
        assertTrue(storedWithSha(STORAGE_ROOT, sha).isEmpty(),
                "source copy must be deleted after checksum verification");
    }

    // ------------------------------------------------------------------

    private long maxJobId(Client admin) throws Exception {
        JsonNode jobs = jsonOf(admin.get("/api/v1/admin/jobs")
                .andExpect(status().isOk()).andReturn());
        long max = 0;
        for (JsonNode j : jobs) {
            max = Math.max(max, j.path("id").asLong());
        }
        return max;
    }

    private long awaitMigrationJob(Client admin, long beforeId) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            JsonNode jobs = jsonOf(admin.get("/api/v1/admin/jobs")
                    .andExpect(status().isOk()).andReturn());
            for (JsonNode j : jobs) {
                if (j.path("id").asLong() > beforeId
                        && "STORAGE_MIGRATION".equals(j.path("type").asText())) {
                    return j.path("id").asLong();
                }
            }
            Thread.sleep(300);
        }
        fail("migration job was not enqueued within 30s");
        return -1;
    }

    private JsonNode awaitJobState(Client admin, long jobId, String wanted) throws Exception {
        long deadline = System.currentTimeMillis() + 90_000;
        String state = "NEW";
        while (System.currentTimeMillis() < deadline) {
            JsonNode job = jsonOf(admin.get("/api/v1/admin/jobs/" + jobId)
                    .andExpect(status().isOk()).andReturn());
            state = job.path("state").asText();
            if (wanted.equals(state)) {
                return job;
            }
            if ("FAILED".equals(state) || "CANCELLED".equals(state)) {
                fail("job " + jobId + " ended " + state + ": " + job.path("errorMessage").asText());
            }
            Thread.sleep(500);
        }
        fail("job " + jobId + " did not reach " + wanted + " within 90s (last state " + state + ")");
        return null;
    }
}

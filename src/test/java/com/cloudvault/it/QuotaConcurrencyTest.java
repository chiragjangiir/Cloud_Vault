package com.cloudvault.it;

import com.cloudvault.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quota engine (§9, §12, §13, §32, §33): server-side reservation before any
 * bytes are written, downgrade that never deletes user data, admin overrides
 * that take effect immediately, and a genuine concurrent-upload race where
 * exactly one of two competing uploads may win.
 */
class QuotaConcurrencyTest extends BaseIntegrationTest {

    private long userId(Client admin, String username) throws Exception {
        JsonNode users = jsonOf(admin.get("/api/v1/admin/users").andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn());
        JsonNode row = findBy(users, "username", username);
        assertNotNull(row, "admin must see user " + username);
        return row.get("id").asLong();
    }

    private void setQuotaOverride(Client admin, long userId, String json) throws Exception {
        admin.postJson("/api/v1/admin/users/" + userId + "/plan", json)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void overQuotaUploadIsRejectedButExistingFilesStayIntact() throws Exception {
        Client admin = admin();
        Client alice = registerAndLogin("quota");
        long aliceId = userId(admin, alice.username);

        byte[] mib = randomBytes(1024 * 1024);
        JsonNode anchor = upload(alice, "anchor-" + unique("") + ".bin", mib, null);
        long anchorId = anchor.get("id").asLong();

        // shrink quota to exactly current usage — downgrade must not delete (§32)
        setQuotaOverride(admin, aliceId, "{\"planCode\":\"FREE\",\"status\":\"ACTIVE\",\"storageOverride\":1048576}");

        // rejection happens BEFORE any bytes are stored (§12)
        int over = uploadStatus(alice, "over-" + unique("") + ".bin", randomBytes(2 * 1024 * 1024));
        assertTrue(over == 507 || over == 413, "over-quota upload must be rejected, got " + over);

        // existing file still downloadable and intact while over quota (§32)
        assertArrayEquals(mib, download(alice, anchorId));
        alice.get("/api/v1/files/" + anchorId).andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // once quota is restored, uploads work again (§33)
        setQuotaOverride(admin, aliceId, "{\"planCode\":\"FREE\",\"status\":\"ACTIVE\",\"storageOverride\":null}");
        assertEquals(201, uploadStatus(alice, "after-" + unique("") + ".bin", mib));
    }

    @Test
    void concurrentUploadsReserveQuotaSoExactlyOneWins() throws Exception {
        Client admin = admin();
        Client alice = registerAndLogin("race");
        long aliceId = userId(admin, alice.username);

        byte[] mib = randomBytes(1024 * 1024);
        upload(alice, "raceanchor-" + unique("") + ".bin", mib, null);   // usage = 1 MiB
        // headroom = 2.5 MiB - 1 MiB = 1.5 MiB → two 1 MiB uploads cannot both fit
        setQuotaOverride(admin, aliceId, "{\"planCode\":\"FREE\",\"status\":\"ACTIVE\",\"storageOverride\":2621440}");

        // two independent authenticated sessions racing real uploads (§13)
        Client first = loginAs(alice.username, PASSWORD);
        Client second = loginAs(alice.username, PASSWORD);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        int s1;
        int s2;
        try {
            Future<Integer> f1 = pool.submit(() -> {
                start.await();
                return uploadStatus(first, "par1-" + unique("") + ".bin", mib);
            });
            Future<Integer> f2 = pool.submit(() -> {
                start.await();
                return uploadStatus(second, "par2-" + unique("") + ".bin", mib);
            });
            start.countDown();
            s1 = f1.get(60, TimeUnit.SECONDS);
            s2 = f2.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            setQuotaOverride(admin, aliceId, "{\"planCode\":\"FREE\",\"status\":\"ACTIVE\",\"storageOverride\":null}");
        }

        long wins = (s1 == 201 ? 1 : 0) + (s2 == 201 ? 1 : 0);
        assertEquals(1, wins, "exactly one concurrent upload may win, got statuses " + s1 + " and " + s2);
        assertTrue((s1 == 201 || s1 == 507 || s1 == 413) && (s2 == 201 || s2 == 507 || s2 == 413),
                "the loser must be rejected by the quota engine, got " + s1 + "/" + s2);

        // usage accounting stayed consistent: a further upload fits after restore
        assertEquals(201, uploadStatus(alice, "postrace-" + unique("") + ".bin", mib));
    }
}

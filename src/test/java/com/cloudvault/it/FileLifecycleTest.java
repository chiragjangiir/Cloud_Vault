package com.cloudvault.it;

import com.cloudvault.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real file lifecycle (§2, §7, §14–§18, §54): physical writes under the
 * configured STORAGE_ROOT, checksum round-trips, range requests, database
 * search, trash/restore/purge with real byte movement, folder hierarchy with
 * cycle prevention, malicious filename rejection and plan file-size limits.
 */
class FileLifecycleTest extends BaseIntegrationTest {

    @Test
    void folderTreeCreateRenameMoveAndCycleGuard() throws Exception {
        Client c = registerAndLogin("fold");
        String docsName = "Docs-" + unique("");
        JsonNode docs = jsonOf(c.postJson("/api/v1/folders",
                "{\"name\":\"" + docsName + "\",\"parentId\":null}")
                .andExpect(status().isCreated()).andReturn());
        long docsId = docs.get("id").asLong();
        JsonNode nested = jsonOf(c.postJson("/api/v1/folders",
                "{\"name\":\"Nested\",\"parentId\":" + docsId + "}")
                .andExpect(status().isCreated()).andReturn());
        long nestedId = nested.get("id").asLong();

        c.patchJson("/api/v1/folders/" + docsId, "{\"name\":\"Renamed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));

        // cycle guard: a folder must not move under its own descendant (§18)
        int cycle = c.postJson("/api/v1/folders/" + docsId + "/move",
                "{\"targetParentId\":" + nestedId + "}")
                .andReturn().getResponse().getStatus();
        assertTrue(cycle == 400 || cycle == 403 || cycle == 409 || cycle == 422,
                "moving a folder into its own subtree must be rejected, got " + cycle);

        // breadcrumbs come from the real hierarchy (§18)
        JsonNode browse = jsonOf(c.get("/api/v1/files?folderId=" + nestedId)
                .andExpect(status().isOk()).andReturn());
        assertTrue(browse.get("breadcrumbs").size() >= 2, "breadcrumbs must reflect nesting");
    }

    @Test
    void uploadWritesRealBytesAndDownloadRoundTrips() throws Exception {
        Client c = registerAndLogin("files");
        byte[] payload = randomBytes(64 * 1024);
        String sha = sha256Hex(payload);
        String marker = "rt" + unique("");

        JsonNode file = upload(c, marker + ".bin", payload, null);
        long id = file.get("id").asLong();
        assertEquals(payload.length, file.get("size").asLong(), "size must come from the real byte stream");
        assertEquals(sha, file.get("checksum").asText(), "checksum must be computed over the real bytes");

        // the bytes physically exist inside the configured STORAGE_ROOT (§2, §7)
        assertFalse(storedWithSha(sha).isEmpty(), "uploaded bytes must exist under " + STORAGE_ROOT);

        // download returns exactly the uploaded bytes (§15)
        assertArrayEquals(payload, download(c, id));

        // HTTP range request for resume/media playback (§15)
        byte[] ranged = mockMvc.perform(c.signed(MockMvcRequestBuilders.get("/api/v1/files/" + id + "/download")
                        .header("Range", "bytes=0-15")))
                .andExpect(status().isPartialContent())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(16, ranged.length, "range request must return exactly 16 bytes");

        // search queries the real database, not a frontend array (§21)
        JsonNode page = jsonOf(c.get("/api/v1/files/search?q=" + marker)
                .andExpect(status().isOk()).andReturn());
        assertTrue(page.get("content").findValuesAsText("id").contains(String.valueOf(id)),
                "search must find the uploaded file in the database");
    }

    @Test
    void moveAndRenameOperateOnRealMetadata() throws Exception {
        Client c = registerAndLogin("mv");
        JsonNode target = jsonOf(c.postJson("/api/v1/folders",
                "{\"name\":\"Target-" + unique("") + "\",\"parentId\":null}")
                .andExpect(status().isCreated()).andReturn());
        long targetId = target.get("id").asLong();

        JsonNode f = upload(c, "mvsrc-" + unique("") + ".bin", randomBytes(2048), null);
        long id = f.get("id").asLong();

        String newName = "renamed-" + unique("") + ".bin";
        c.putJson("/api/v1/files/" + id, "{\"name\":\"" + newName + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
        JsonNode afterRename = jsonOf(c.get("/api/v1/files/" + id).andExpect(status().isOk()).andReturn());
        assertEquals(newName, afterRename.get("name").asText(), "rename must persist in the database");

        JsonNode moved = jsonOf(c.postJson("/api/v1/files/" + id + "/move",
                "{\"folderId\":" + targetId + "}")
                .andExpect(status().isOk()).andReturn());
        assertEquals(targetId, moved.get("folderId").asLong(), "file must really move folders");

        // folder move (§18)
        JsonNode a = jsonOf(c.postJson("/api/v1/folders",
                "{\"name\":\"A-" + unique("") + "\",\"parentId\":null}")
                .andExpect(status().isCreated()).andReturn());
        JsonNode b = jsonOf(c.postJson("/api/v1/folders",
                "{\"name\":\"B-" + unique("") + "\",\"parentId\":null}")
                .andExpect(status().isCreated()).andReturn());
        c.postJson("/api/v1/folders/" + a.get("id").asLong() + "/move",
                "{\"targetParentId\":" + b.get("id").asLong() + "}")
                .andExpect(status().isOk());
    }

    @Test
    void trashRestoreAndPurgeDeleteRealObjects() throws Exception {
        Client c = registerAndLogin("trash");
        byte[] payload = randomBytes(24 * 1024);
        String sha = sha256Hex(payload);
        JsonNode f = upload(c, "trash-" + unique("") + ".bin", payload, null);
        long id = f.get("id").asLong();
        assertFalse(storedWithSha(sha).isEmpty());

        // soft delete: metadata hidden, physical object retained (§16, §17)
        c.delete("/api/v1/files/" + id).andExpect(status().isNoContent());
        c.get("/api/v1/files/" + id).andExpect(status().isNotFound());
        assertEquals(404, c.get("/api/v1/files/" + id + "/download")
                .andReturn().getResponse().getStatus(), "trashed file must not download");
        assertFalse(storedWithSha(sha).isEmpty(), "trash keeps the physical object");

        // restore: accessible again with identical bytes (§17)
        c.post("/api/v1/files/" + id + "/restore").andExpect(status().isNoContent());
        assertArrayEquals(payload, download(c, id));

        // permanent delete: physical object really removed (§16)
        c.delete("/api/v1/files/" + id).andExpect(status().isNoContent());
        c.delete("/api/v1/files/" + id + "/permanent").andExpect(status().isNoContent());
        assertTrue(storedWithSha(sha).isEmpty(), "purge must remove the physical object");
        c.get("/api/v1/files/" + id).andExpect(status().isNotFound());
    }

    @Test
    void maliciousFilenamesAreRejectedAndNothingEscapesTheRoot() throws Exception {
        Client c = registerAndLogin("evil");
        String[] evils = {"../evil.txt", "..\\evil.txt", "/etc/passwd", "a/b.txt", "..", "."};
        for (String evil : evils) {
            int s = mockMvc.perform(c.signed(MockMvcRequestBuilders.post("/api/v1/files")
                            .queryParam("name", evil)
                            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                            .content("pwn".getBytes(StandardCharsets.UTF_8))))
                    .andReturn().getResponse().getStatus();
            assertEquals(422, s, "must reject malicious name: " + evil);
        }
        // NUL byte may be refused by the container (400) or the app (422)
        int nul = mockMvc.perform(c.signed(MockMvcRequestBuilders.post("/api/v1/files")
                        .queryParam("name", "bad\0name")
                        .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                        .content("pwn".getBytes(StandardCharsets.UTF_8))))
                .andReturn().getResponse().getStatus();
        assertTrue(nul == 400 || nul == 422, "NUL byte filename must be rejected, got " + nul);

        // nothing escaped the storage root (§8)
        Path root = STORAGE_ROOT;
        assertFalse(Files.exists(root.resolveSibling("evil.txt")), "no file may escape STORAGE_ROOT");
        assertFalse(Files.exists(root.resolve("../evil.txt")), "no file may escape STORAGE_ROOT");
        try (var walk = Files.walk(root.getParent(), 1)) {
            assertTrue(walk.noneMatch(p -> p.getFileName().toString().equals("evil.txt")),
                    "evil.txt must not exist outside the storage root");
        }
    }

    @Test
    void planMaxFileSizeIsEnforcedServerSide() throws Exception {
        Client admin = admin();
        JsonNode plans = jsonOf(admin.get("/api/v1/admin/plans").andExpect(status().isOk()).andReturn());
        JsonNode free = findBy(plans, "code", "FREE");
        assertNotNull(free, "FREE plan must exist");
        long planId = free.get("id").asLong();
        long originalMax = free.get("maxFileBytes").asLong();
        try {
            admin.putJson("/api/v1/admin/plans/" + planId, "{\"maxFileBytes\":1024}")
                    .andExpect(status().isOk());
            Client u = registerAndLogin("big");
            int s = uploadStatus(u, "big-" + unique("") + ".bin", randomBytes(4096));
            assertEquals(413, s, "upload larger than the plan's maxFileBytes must be rejected with 413");
        } finally {
            admin.putJson("/api/v1/admin/plans/" + planId,
                    "{\"maxFileBytes\":" + originalMax + "}").andExpect(status().isOk());
        }
    }
}

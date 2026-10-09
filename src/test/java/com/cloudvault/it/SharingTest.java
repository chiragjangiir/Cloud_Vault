package com.cloudvault.it;

import com.cloudvault.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sharing (§19, §20, §36): cryptographically random tokens mapped to real
 * share records, anonymous access through the API, immediate revocation,
 * server-enforced expiry, and USER_SHARE authorization for a second account.
 */
class SharingTest extends BaseIntegrationTest {

    @Test
    void publicShareLifecycleIsServerEnforced() throws Exception {
        Client alice = registerAndLogin("share");
        byte[] payload = randomBytes(16 * 1024);
        JsonNode f = upload(alice, "share-" + unique("") + ".bin", payload, null);
        long fileId = f.get("id").asLong();

        JsonNode share = jsonOf(alice.postJson("/api/v1/shares",
                        "{\"fileId\":" + fileId + ",\"type\":\"PUBLIC_LINK\",\"permission\":\"DOWNLOAD\"}")
                .andExpect(status().isCreated()).andReturn());
        String token = share.get("token").asText();
        long shareId = share.get("id").asLong();
        assertTrue(token.length() >= 20, "share token must be cryptographically long, got " + token.length());

        // fully anonymous access through the API (§19)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + token))
                .andExpect(status().isOk());
        byte[] shared = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + token + "/download"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(payload, shared, "anonymous shared download must return the real bytes");

        // token guessing must fail (§36)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + "a".repeat(40)))
                .andExpect(status().isNotFound());

        // revocation takes effect immediately, server-side (§19)
        alice.delete("/api/v1/shares/" + shareId).andExpect(status().isNoContent());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + token + "/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiryIsEnforcedByTheServerNotTheUi() throws Exception {
        Client alice = registerAndLogin("exp");
        JsonNode f = upload(alice, "exp-" + unique("") + ".bin", randomBytes(4096), null);
        String expiresAt = Instant.now().plusSeconds(3).toString();

        JsonNode share = jsonOf(alice.postJson("/api/v1/shares",
                        "{\"fileId\":" + f.get("id").asLong()
                                + ",\"type\":\"PUBLIC_LINK\",\"permission\":\"VIEW\",\"expiresAt\":\"" + expiresAt + "\"}")
                .andExpect(status().isCreated()).andReturn());
        String token = share.get("token").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + token))
                .andExpect(status().isOk());
        Thread.sleep(4000); // let the real expiry instant pass
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/public/shares/" + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void userShareGrantsThenRevokesRealCrossAccountAccess() throws Exception {
        Client alice = registerAndLogin("owner");
        Client bob = registerAndLogin("recip");
        byte[] payload = randomBytes(12 * 1024);
        JsonNode f = upload(alice, "us-" + unique("") + ".bin", payload, null);
        long fileId = f.get("id").asLong();

        JsonNode share = jsonOf(alice.postJson("/api/v1/shares",
                        "{\"fileId\":" + fileId + ",\"type\":\"USER_SHARE\",\"permission\":\"DOWNLOAD\",\"recipient\":\""
                                + bob.username + "\"}")
                .andExpect(status().isCreated()).andReturn());

        // recipient sees the incoming share in their own account
        JsonNode incoming = jsonOf(bob.get("/api/v1/shares/incoming")
                .andExpect(status().isOk()).andReturn());
        assertTrue(incoming.toString().contains("\"fileId\":" + fileId),
                "bob must see the incoming share for file " + fileId);

        // the recipient downloads the real bytes of someone else's file —
        // authorized purely by the share record (§19, §35)
        assertArrayEquals(payload, download(bob, fileId));

        // revocation removes access for the recipient
        alice.delete("/api/v1/shares/" + share.get("id").asLong()).andExpect(status().isNoContent());
        int after = bob.get("/api/v1/files/" + fileId + "/download")
                .andReturn().getResponse().getStatus();
        assertTrue(after == 403 || after == 404,
                "revoked share must stop cross-account access, got " + after);
    }
}

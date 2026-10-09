package com.cloudvault.it;

import com.cloudvault.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authentication and authorization (§34–§36): real sessions, real password
 * hashing, server-side role enforcement, IDOR denial, rate limiting, and
 * credential-material exposure checks.
 */
class AuthSecurityTest extends BaseIntegrationTest {

    private static String creds(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    @Test
    void registrationLoginAndLogoutDriveRealSessions() throws Exception {
        Client c = registerAndLogin("lifecycle");
        c.get("/api/v1/auth/me").andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(c.username))
                .andExpect(jsonPath("$.plan").value("FREE"));
        c.post("/api/v1/auth/logout").andExpect(status().isNoContent());
        c.get("/api/v1/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationAndWeakPasswordsAreRejected() throws Exception {
        Client c = client();
        String username = unique("dup");
        c.postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s@example.test","password":"%s","displayName":"x"}
                """.formatted(username, username, PASSWORD))
                .andExpect(status().isCreated());
        // duplicate identity → 409
        c.postJson("/api/v1/auth/register", """
                {"username":"%s","email":"other@example.test","password":"%s","displayName":"x"}
                """.formatted(username, PASSWORD))
                .andExpect(status().isConflict());
        // weak password → 422, never stored
        c.postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s@example.test","password":"short","displayName":"x"}
                """.formatted(unique("weak"), unique("weak")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anonymousRequestsAreUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files")).andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/shares")).andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/overview")).andExpect(status().isUnauthorized());
        // browser pages redirect to the login page instead of leaking content
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/users")).andExpect(status().is3xxRedirection());
        mockMvc.perform(MockMvcRequestBuilders.get("/app")).andExpect(status().is3xxRedirection());
    }

    @Test
    void authenticatedUserCannotReachAdminSurface() throws Exception {
        Client u = registerAndLogin("plain");
        u.get("/api/v1/admin/overview").andExpect(status().isForbidden());
        u.get("/api/v1/admin/users").andExpect(status().isForbidden());
        u.get("/api/v1/admin/storage/locations").andExpect(status().isForbidden());
        u.get("/api/v1/admin/jobs").andExpect(status().isForbidden());
        u.get("/admin").andExpect(status().isForbidden());
        u.get("/admin/users").andExpect(status().isForbidden());
    }

    @Test
    void crossUserFileAccessIsInvisible() throws Exception {
        Client alice = registerAndLogin("idora");
        JsonNode file = upload(alice, "idor-" + unique("f") + ".bin", randomBytes(4096), null);
        long id = file.get("id").asLong();

        Client bob = registerAndLogin("idorb");
        bob.get("/api/v1/files/" + id).andExpect(status().isNotFound());
        bob.get("/api/v1/files/" + id + "/download").andExpect(status().isNotFound());
        bob.delete("/api/v1/files/" + id).andExpect(status().isNotFound());
        bob.delete("/api/v1/files/" + id + "/permanent").andExpect(status().isNotFound());
        // owner still has full access
        alice.get("/api/v1/files/" + id).andExpect(status().isOk());
    }

    @Test
    void formLoginIsRateLimitedAfterConfiguredAttempts() throws Exception {
        Client c = client();
        String probe = unique("rl");
        int last = -1;
        for (int i = 0; i < 5; i++) {
            last = c.form("/login", "username", probe, "password", "WrongPass!999")
                    .andReturn().getResponse().getStatus();
        }
        // configured limit is 3 per 10 minutes per IP+username (see BaseIntegrationTest)
        assertEquals(429, last, "5 failed form logins with a limit of 3 must end in 429, got " + last);
    }

    @Test
    void passwordChangeInvalidatesTheOldPassword() throws Exception {
        Client c = registerAndLogin("pwdchg");
        c.postJson("/api/v1/auth/password/change",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewPass!987654\"}")
                .andExpect(status().isNoContent());

        Client old = client();
        old.postJson("/api/v1/auth/login", creds(c.username, PASSWORD))
                .andExpect(status().isUnauthorized());
        Client fresh = client();
        fresh.postJson("/api/v1/auth/login", creds(c.username, "NewPass!987654"))
                .andExpect(status().isOk());
    }

    @Test
    void apiResponsesNeverExposeCredentialMaterial() throws Exception {
        Client admin = admin();
        String[] endpoints = {
                "/api/v1/admin/jobs", "/api/v1/admin/users", "/api/v1/admin/subscriptions",
                "/api/v1/admin/audit", "/api/v1/admin/overview", "/api/v1/auth/me"
        };
        for (String ep : endpoints) {
            String body = admin.get(ep).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertFalse(body.contains("passwordHash"), ep + " must not expose passwordHash");
            assertFalse(body.contains("$2a$") || body.contains("$2b$") || body.contains("$2y$"),
                    ep + " must not expose bcrypt hashes");
        }
        // A populated share list must not leak the owner entity either (§62).
        Client u = registerAndLogin("noleak");
        JsonNode f = upload(u, "noleak-" + unique("f") + ".bin", randomBytes(1024), null);
        u.postJson("/api/v1/shares",
                "{\"fileId\":" + f.get("id").asLong() + ",\"type\":\"PUBLIC_LINK\",\"permission\":\"DOWNLOAD\"}")
                .andExpect(status().isCreated());
        String shares = u.get("/api/v1/shares").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertNotNull(shares);
        assertFalse(shares.contains("passwordHash"), "shares must not expose passwordHash");
        assertFalse(shares.contains("$2a$"), "shares must not expose bcrypt hashes");
    }
}

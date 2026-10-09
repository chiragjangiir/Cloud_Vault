package com.cloudvault.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared Spring context for the integration test suite.
 *
 * <p>These tests exercise REAL infrastructure as the spec demands (§53–§56):</p>
 * <ul>
 *   <li>a real PostgreSQL database ({@code cloudvault_test}) — dropped to a
 *       pristine schema at suite start so Flyway performs the genuine
 *       migrations on an empty database and no state leaks between runs;</li>
 *   <li>a real temporary STORAGE_ROOT on the local filesystem, so uploads,
 *       downloads, checksums, trash and migration hit actual bytes.</li>
 * </ul>
 *
 * <p>No mocks, no H2, no stubbed services.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    protected static final ObjectMapper JSON = new ObjectMapper();
    protected static final String ADMIN_USER = "admin";
    protected static final String ADMIN_PASS = "Admin!Pass123";
    protected static final String PASSWORD = "Str0ngPass!42";

    /** Hard safety rail: the suite may only ever wipe its own database. */
    private static final String TEST_DB_URL = "jdbc:postgresql://localhost:5432/cloudvault_test";
    private static final String TEST_DB_USER = "cloudvault";
    private static final String TEST_DB_PASSWORD = "cloudvault";

    /** Real on-disk storage root shared by the whole suite. */
    protected static final Path STORAGE_ROOT = initStorage();

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    protected MockMvc mockMvc;

    private static Path initStorage() {
        resetTestDatabase();
        Path root = Paths.get("target", "it-storage").toAbsolutePath().normalize();
        try {
            if (Files.exists(root)) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
            }
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot prepare test storage root: " + root, e);
        }
        return root;
    }

    private static void resetTestDatabase() {
        if (!TEST_DB_URL.contains("cloudvault_test")) {
            throw new IllegalStateException("Refusing to reset a database that is not the test database: " + TEST_DB_URL);
        }
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD);
             Statement st = conn.createStatement()) {
            st.execute("DROP SCHEMA public CASCADE");
            st.execute("CREATE SCHEMA public");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot reset test database " + TEST_DB_URL + " — create it first, e.g. "
                            + "createdb -O cloudvault cloudvault_test", e);
        }
    }

    @DynamicPropertySource
    static void cloudVaultProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_DB_URL);
        registry.add("spring.datasource.username", () -> TEST_DB_USER);
        registry.add("spring.datasource.password", () -> TEST_DB_PASSWORD);
        registry.add("app.storage.root", () -> STORAGE_ROOT.toString());
        registry.add("app.admin.username", () -> ADMIN_USER);
        registry.add("app.admin.password", () -> ADMIN_PASS);
        registry.add("app.public-url", () -> "http://localhost:8080");
        // Generous limits keep the suite deterministic; the dedicated
        // rate-limit test exercises the form-login limiter configured low below.
        registry.add("app.rate-limit.register-per-hour", () -> "10000");
        registry.add("app.rate-limit.api-login-per-10min", () -> "10000");
        registry.add("app.rate-limit.reset-per-hour", () -> "10000");
        registry.add("app.rate-limit.share-per-hour", () -> "10000");
        registry.add("app.rate-limit.upload-per-minute", () -> "10000");
        registry.add("app.rate-limit.login-per-10min", () -> "3");
    }

    // ------------------------------------------------------------------
    // Client (one authenticated browser-like session with its CSRF cookie)
    // ------------------------------------------------------------------

    protected final class Client {
        private final MockHttpSession session = new MockHttpSession();
        private String xsrf = "";
        public String username;

        public MockHttpSession session() { return session; }
        public String xsrf() { return xsrf; }

        /** Applies this client's session and CSRF cookie/header to any request. */
        public MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder b) {
            return b.session(session)
                    .cookie(new Cookie("XSRF-TOKEN", xsrf))
                    .header("X-XSRF-TOKEN", xsrf);
        }

        public ResultActions get(String uri) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.get(uri)));
        }

        public ResultActions post(String uri) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.post(uri)));
        }

        public ResultActions delete(String uri) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.delete(uri)));
        }

        public ResultActions postJson(String uri, String body) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.post(uri))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        public ResultActions putJson(String uri, String body) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.put(uri))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        public ResultActions patchJson(String uri, String body) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.patch(uri))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        public ResultActions upload(String uri, byte[] payload) throws Exception {
            return mockMvc.perform(signed(MockMvcRequestBuilders.post(uri))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).content(payload));
        }

        public ResultActions form(String uri, String... params) throws Exception {
            MockHttpServletRequestBuilder b = signed(MockMvcRequestBuilders.post(uri))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            for (int i = 0; i + 1 < params.length; i += 2) {
                b.param(params[i], params[i + 1]);
            }
            return mockMvc.perform(b);
        }
    }

    /** A fresh anonymous session with a valid XSRF cookie (like opening /login). */
    protected Client client() throws Exception {
        Client c = new Client();
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.get("/login").session(c.session)).andReturn();
        Cookie cookie = r.getResponse().getCookie("XSRF-TOKEN");
        Assertions.assertNotNull(cookie, "GET /login must issue an XSRF-TOKEN cookie");
        c.xsrf = cookie.getValue();
        return c;
    }

    protected Client registerUser(String prefix) throws Exception {
        Client c = client();
        c.username = unique(prefix);
        c.postJson("/api/v1/auth/register", """
                {"username":"%s","email":"%s@example.test","password":"%s","displayName":"%s"}
                """.formatted(c.username, c.username, PASSWORD, c.username))
                .andExpect(status().isCreated());
        return c;
    }

    protected Client loginAs(String username, String password) throws Exception {
        Client c = client();
        c.username = username;
        c.postJson("/api/v1/auth/login",
                "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password))
                .andExpect(status().isOk());
        return c;
    }

    protected Client registerAndLogin(String prefix) throws Exception {
        Client c = registerUser(prefix);
        c.postJson("/api/v1/auth/login",
                "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(c.username, PASSWORD))
                .andExpect(status().isOk());
        return c;
    }

    protected Client admin() throws Exception {
        return loginAs(ADMIN_USER, ADMIN_PASS);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    protected static String unique(String prefix) {
        return prefix + SEQ.incrementAndGet() + Long.toString(System.nanoTime(), 36);
    }

    protected JsonNode upload(Client c, String name, byte[] payload, Long folderId) throws Exception {
        String uri = "/api/v1/files?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                + (folderId == null ? "" : "&folderId=" + folderId);
        MvcResult r = c.upload(uri, payload).andExpect(status().isCreated()).andReturn();
        return jsonOf(r);
    }

    protected int uploadStatus(Client c, String name, byte[] payload) throws Exception {
        String uri = "/api/v1/files?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        return c.upload(uri, payload).andReturn().getResponse().getStatus();
    }

    protected byte[] download(Client c, long fileId) throws Exception {
        return c.get("/api/v1/files/" + fileId + "/download")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    protected static JsonNode jsonOf(MvcResult result) {
        try {
            return JSON.readTree(result.getResponse().getContentAsString());
        } catch (IOException e) {
            throw new UncheckedIOException("Response was not valid JSON (HTTP "
                    + result.getResponse().getStatus() + ")", e);
        }
    }

    protected static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    /** Files physically present under {@code root} whose SHA-256 matches. */
    protected static List<Path> storedWithSha(Path root, String sha256) {
        List<Path> found = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    if (sha256Hex(Files.readAllBytes(p)).equals(sha256)) found.add(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }

    protected static List<Path> storedWithSha(String sha256) {
        return storedWithSha(STORAGE_ROOT, sha256);
    }

    protected static JsonNode findById(JsonNode array, long id) {
        for (JsonNode n : array) {
            if (n.path("id").asLong(-1) == id) return n;
        }
        return null;
    }

    protected static JsonNode findBy(JsonNode array, String field, String value) {
        for (JsonNode n : array) {
            if (value.equals(n.path(field).asText())) return n;
        }
        return null;
    }
}

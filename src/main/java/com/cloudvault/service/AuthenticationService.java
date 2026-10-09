package com.cloudvault.service;

import com.cloudvault.domain.Folder;
import com.cloudvault.domain.PasswordResetToken;
import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.repository.FolderRepository;
import com.cloudvault.repository.PasswordResetTokenRepository;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.web.error.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Real account lifecycle: registration with bcrypt hashing, password change,
 * and a token-based password reset flow (raw token only ever emailed; only
 * its SHA-256 hash is persisted).
 */
@Service
public class AuthenticationService {

    public static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository users;
    private final FolderRepository folders;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionService subscriptionService;
    private final MailService mailService;
    private final AuditService auditService;

    public AuthenticationService(UserRepository users,
                                 FolderRepository folders,
                                 PasswordResetTokenRepository tokens,
                                 PasswordEncoder passwordEncoder,
                                 SubscriptionService subscriptionService,
                                 MailService mailService,
                                 AuditService auditService) {
        this.users = users;
        this.folders = folders;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionService = subscriptionService;
        this.mailService = mailService;
        this.auditService = auditService;
    }

    private static void validatePassword(String password, String username, String email) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.unprocessable("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (username != null && password.equalsIgnoreCase(username)) {
            throw ApiException.unprocessable("Password must not match the username");
        }
        if (email != null && password.contains(email.split("@")[0])) {
            throw ApiException.unprocessable("Password must not contain the email address");
        }
    }

    @Transactional
    public User register(String username, String email, String password, String displayName) {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,64}")) {
            throw ApiException.unprocessable("Username must be 3-64 characters (letters, digits, dot, dash, underscore)");
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.unprocessable("A valid email address is required");
        }
        validatePassword(password, username, email);
        if (users.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Username is already taken");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName == null || displayName.isBlank() ? username : displayName.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.ACTIVE);
        user = users.save(user);

        // Real initial state: FREE subscription + root folder + usage row.
        subscriptionService.assignPlan(user, SubscriptionService.FREE, Subscription.Status.ACTIVE, null, "Assigned at registration");
        Folder root = new Folder();
        root.setOwner(user);
        root.setName("Home");
        root.setParent(null);
        folders.save(root);
        return user;
    }

    /**
     * API login: verifies credentials against the stored bcrypt hash and
     * applies the same lockout rules as form login.
     */
    @Transactional
    public User authenticateForApi(String username, String password) {
        User user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_DISABLED", "Account is disabled");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_LOCKED", "Account temporarily locked due to failed logins");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            if (failures >= com.cloudvault.security.CloudVaultAuthenticationProvider.MAX_FAILURES) {
                user.setLockedUntil(Instant.now().plus(
                        com.cloudvault.security.CloudVaultAuthenticationProvider.LOCK_DURATION));
            }
            users.save(user);
            throw ApiException.unauthorized("Invalid username or password");
        }
        if (user.getFailedLoginCount() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            users.save(user);
        }
        return user;
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            auditService.record("PASSWORD_CHANGE_FAILED", "USER", String.valueOf(user.getId()), "Current password incorrect");
            throw ApiException.unprocessable("Current password is incorrect");
        }
        validatePassword(newPassword, user.getUsername(), user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        auditService.record("PASSWORD_CHANGED", "USER", String.valueOf(user.getId()), null);
    }

    /**
     * Starts the reset flow. Returns true when a token was created. The raw
     * token is emailed immediately and never stored.
     */
    @Transactional
    public boolean requestReset(String identifier) {
        User user = users.findByUsernameIgnoreCase(identifier)
                .or(() -> users.findByEmailIgnoreCase(identifier))
                .orElse(null);
        if (user == null || user.getStatus() != User.Status.ACTIVE) {
            return false;
        }
        if (!mailService.isConfigured()) {
            return false;
        }
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
        tokens.save(token);
        mailService.sendPasswordReset(user.getEmail(), rawToken);
        auditService.record("PASSWORD_RESET_REQUESTED", "USER", String.valueOf(user.getId()), null);
        return true;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.unprocessable("Reset token is required");
        }
        PasswordResetToken token = tokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> ApiException.notFound("Reset link is invalid or has expired"));
        if (!token.isValid(Instant.now())) {
            throw ApiException.notFound("Reset link is invalid or has expired");
        }
        User user = token.getUser();
        validatePassword(newPassword, user.getUsername(), user.getEmail());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        token.setUsedAt(Instant.now());
        tokens.save(token);
        auditService.record("PASSWORD_RESET_COMPLETED", "USER", String.valueOf(user.getId()), null);
    }

    /** Administrator-issued password reset (audited). */
    @Transactional
    public void adminResetPassword(User target, String newPassword) {
        validatePassword(newPassword, target.getUsername(), target.getEmail());
        target.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(target);
        auditService.record("PASSWORD_RESET_BY_ADMIN", "USER", String.valueOf(target.getId()), null);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

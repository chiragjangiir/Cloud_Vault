package com.cloudvault.web.api;

import com.cloudvault.domain.User;
import com.cloudvault.service.AuthenticationService;
import com.cloudvault.service.QuotaService;
import com.cloudvault.service.RateLimitService;
import com.cloudvault.service.SubscriptionService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class ApiAuthController {

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 64) String username,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 10, max = 200) String password,
                                  @Size(max = 128) String displayName) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record PasswordChangeRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 10, max = 200) String newPassword) {}

    public record ResetRequest(@NotBlank String identifier) {}

    public record ResetConfirmRequest(@NotBlank String token,
                                      @NotBlank @Size(min = 10, max = 200) String newPassword) {}

    public record UserView(long id, String username, String email, String displayName,
                           String role, String plan, String subscriptionStatus,
                           long quotaBytes, long usedBytes) {}

    private final AuthenticationService authenticationService;
    private final SubscriptionService subscriptionService;
    private final QuotaService quotaService;
    private final RateLimitService rateLimitService;
    private final CurrentUser currentUser;
    private final com.cloudvault.security.SessionTrackingListener sessionTracking;
    private final int registerPerHour;
    private final int apiLoginPer10min;
    private final int resetPerHour;

    public ApiAuthController(AuthenticationService authenticationService,
                             SubscriptionService subscriptionService,
                             QuotaService quotaService,
                             RateLimitService rateLimitService,
                             CurrentUser currentUser,
                             com.cloudvault.security.SessionTrackingListener sessionTracking,
                             @Value("${app.rate-limit.register-per-hour:10}") int registerPerHour,
                             @Value("${app.rate-limit.api-login-per-10min:10}") int apiLoginPer10min,
                             @Value("${app.rate-limit.reset-per-hour:5}") int resetPerHour) {
        this.authenticationService = authenticationService;
        this.subscriptionService = subscriptionService;
        this.quotaService = quotaService;
        this.rateLimitService = rateLimitService;
        this.currentUser = currentUser;
        this.sessionTracking = sessionTracking;
        this.registerPerHour = registerPerHour;
        this.apiLoginPer10min = apiLoginPer10min;
        this.resetPerHour = resetPerHour;
    }

    private UserView view(User user) {
        SubscriptionService.Limits limits = quotaService.limitsFor(user);
        return new UserView(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRole().name(), limits.planCode(),
                limits.subscriptionStatus() == null ? null : limits.subscriptionStatus().name(),
                limits.quotaBytes(), limits.usedBytes());
    }

    @PostMapping("/register")
    public ResponseEntity<UserView> register(@Valid @RequestBody RegisterRequest req,
                                             HttpServletRequest request) {
        rateLimitService.consume("register", request.getRemoteAddr(), registerPerHour, Duration.ofHours(1));
        User user = authenticationService.register(req.username(), req.email(), req.password(), req.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(view(user));
    }

    /**
     * Session-based API login (also usable by API clients with a cookie).
     * Credentials are authenticated against the real database via Spring
     * Security's provider; failures return 401 without user enumeration.
     */
    @PostMapping("/login")
    public UserView login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        rateLimitService.consume("api-login", request.getRemoteAddr() + "|" + req.username().toLowerCase(),
                apiLoginPer10min, Duration.ofMinutes(10));
        User user = authenticationService.authenticateForApi(req.username(), req.password());
        var details = new com.cloudvault.security.CloudVaultUserDetails(user);
        var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        HttpSession session = request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        sessionTracking.sessionAuthenticated(session);
        return view(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserView me() {
        return view(currentUser.require());
    }

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        User user = currentUser.require();
        authenticationService.changePassword(user, req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Map<String, String>> resetRequest(@Valid @RequestBody ResetRequest req,
                                                            HttpServletRequest request) {
        rateLimitService.consume("reset", request.getRemoteAddr(), resetPerHour, Duration.ofHours(1));
        boolean configured = authenticationService.requestReset(req.identifier());
        if (!configured) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RESET_UNAVAILABLE",
                    "Password reset is not available; contact your administrator");
        }
        // Always 202 for existing or unknown identifiers (no enumeration).
        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetConfirmRequest req) {
        authenticationService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}

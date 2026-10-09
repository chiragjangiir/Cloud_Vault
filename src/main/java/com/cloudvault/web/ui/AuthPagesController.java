package com.cloudvault.web.ui;

import com.cloudvault.service.AuthenticationService;
import com.cloudvault.service.RateLimitService;
import com.cloudvault.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;

/** Public authentication pages: login, register, forgot/reset password. */
@Controller
public class AuthPagesController {

    private final AuthenticationService authenticationService;
    private final RateLimitService rateLimitService;
    private final ObjectProvider<org.springframework.security.core.session.SessionRegistry> sessionRegistry;
    private final int registerPerHour;
    private final int resetPerHour;

    public AuthPagesController(AuthenticationService authenticationService,
                               RateLimitService rateLimitService,
                               ObjectProvider<org.springframework.security.core.session.SessionRegistry> sessionRegistry,
                               @org.springframework.beans.factory.annotation.Value("${app.rate-limit.register-per-hour:10}") int registerPerHour,
                               @org.springframework.beans.factory.annotation.Value("${app.rate-limit.reset-per-hour:5}") int resetPerHour) {
        this.authenticationService = authenticationService;
        this.rateLimitService = rateLimitService;
        this.sessionRegistry = sessionRegistry;
        this.registerPerHour = registerPerHour;
        this.resetPerHour = resetPerHour;
    }

    @GetMapping({"/", "/login"})
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String displayName,
                           HttpServletRequest request,
                           RedirectAttributes ra) {
        rateLimitService.consume("register", request.getRemoteAddr(), registerPerHour, Duration.ofHours(1));
        try {
            authenticationService.register(username, email, password, displayName);
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
        ra.addFlashAttribute("message", "Account created. Please sign in.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPage() {
        return "auth/forgot";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String identifier, HttpServletRequest request, RedirectAttributes ra) {
        rateLimitService.consume("reset", request.getRemoteAddr(), resetPerHour, Duration.ofHours(1));
        boolean sent = authenticationService.requestReset(identifier);
        if (sent) {
            ra.addFlashAttribute("message", "If that account exists, a reset link has been sent by email.");
        } else {
            ra.addFlashAttribute("error",
                    "Password reset is unavailable (email is not configured) or the account cannot be reset. Contact your administrator.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token == null ? "" : token);
        return "auth/reset";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token, @RequestParam String password,
                        @RequestParam String confirmPassword, RedirectAttributes ra) {
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/reset-password?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            authenticationService.resetPassword(token, password);
        } catch (ApiException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password";
        }
        ra.addFlashAttribute("message", "Password updated. Please sign in.");
        return "redirect:/login";
    }
}

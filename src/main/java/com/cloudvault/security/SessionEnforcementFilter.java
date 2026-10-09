package com.cloudvault.security;

import com.cloudvault.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces session revocation: every authenticated request must map to a live
 * row in {@code user_sessions}. Deleting the row (user "sign out all", admin
 * revoke) therefore terminates the session on its next request.
 */
@Component
public class SessionEnforcementFilter extends OncePerRequestFilter {

    private final UserSessionRepository sessions;

    public SessionEnforcementFilter(UserSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof CloudVaultUserDetails;

        if (authenticated) {
            HttpSession session = request.getSession(false);
            if (session != null && !sessions.existsById(session.getId())) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                if (request.getRequestURI().startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/problem+json");
                    response.getWriter().write(
                            "{\"title\":\"SESSION_REVOKED\",\"status\":401,\"detail\":\"Session has been revoked\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/login?revoked");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

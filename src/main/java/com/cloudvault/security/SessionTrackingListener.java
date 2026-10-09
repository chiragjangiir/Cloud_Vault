package com.cloudvault.security;

import com.cloudvault.domain.User;
import com.cloudvault.domain.UserSession;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.repository.UserSessionRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Mirrors live HTTP sessions into the {@code user_sessions} table so users and
 * administrators can see (and terminate) active sessions.
 */
@Component
public class SessionTrackingListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionTrackingListener.class);

    private final UserSessionRepository sessions;
    private final UserRepository users;

    public SessionTrackingListener(UserSessionRepository sessions, UserRepository users) {
        this.sessions = sessions;
        this.users = users;
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // Row is written on authentication (see sessionAuthenticated).
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sessions.findById(event.getSession().getId()).ifPresent(sessions::delete);
    }

    /** Called after successful authentication to record the session. */
    public void sessionAuthenticated(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CloudVaultUserDetails details)) {
            return;
        }
        User user = users.findById(details.getId()).orElse(null);
        if (user == null) return;
        UserSession row = new UserSession();
        row.setId(session.getId());
        row.setUser(user);
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            row.setIpAddress(attrs.getRequest().getRemoteAddr());
            String ua = attrs.getRequest().getHeader("User-Agent");
            if (ua != null) row.setUserAgent(ua.substring(0, Math.min(ua.length(), 300)));
        }
        sessions.save(row);
        log.debug("Session recorded for user {}", user.getUsername());
    }
}

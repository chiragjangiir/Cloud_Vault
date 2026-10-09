package com.cloudvault.service;

import com.cloudvault.domain.AuditLog;
import com.cloudvault.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records real security-relevant actions. Never records secrets (passwords,
 * tokens, share tokens).
 */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void record(String action, String targetType, String targetId, String details) {
        AuditLog entry = new AuditLog();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User u) {
            entry.setActorUsername(u.getUsername());
        }
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetails(details != null && details.length() > 2000 ? details.substring(0, 2000) : details);
        entry.setIpAddress(currentIp());
        repo.save(entry);
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String fwd = request.getHeader("X-Forwarded-For");
            if (fwd != null && !fwd.isBlank()) {
                return fwd.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    public Page<AuditLog> search(String action, String actor, Pageable pageable) {
        String a = (action == null || action.isBlank()) ? null : action;
        String u = (actor == null || actor.isBlank()) ? null : actor.toLowerCase(java.util.Locale.ROOT);
        return repo.search(a, u, pageable);
    }
}

package com.cloudvault.web;

import com.cloudvault.domain.User;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.security.CloudVaultUserDetails;
import com.cloudvault.web.error.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated user from the security context. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    private CloudVaultUserDetails principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CloudVaultUserDetails details)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return details;
    }

    public User require() {
        User user = users.findById(principal().getId())
                .orElseThrow(() -> ApiException.unauthorized("Authentication required"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "ACCOUNT_DISABLED", "Account is disabled");
        }
        return user;
    }

    public User requireAdmin() {
        User user = require();
        if (user.getRole() != User.Role.ADMIN) {
            throw ApiException.forbidden("Administrator access required");
        }
        return user;
    }

    public Long id() {
        return principal().getId();
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

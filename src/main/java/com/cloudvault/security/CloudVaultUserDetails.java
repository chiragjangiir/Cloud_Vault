package com.cloudvault.security;

import com.cloudvault.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class CloudVaultUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean admin;
    private final Instant lockedUntil;

    public CloudVaultUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.email = user.getEmail();
        this.enabled = user.getStatus() == User.Status.ACTIVE;
        this.lockedUntil = user.getLockedUntil();
        this.accountNonLocked = lockedUntil == null || lockedUntil.isBefore(Instant.now());
        this.admin = user.getRole() == User.Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER"));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return admin; }
}

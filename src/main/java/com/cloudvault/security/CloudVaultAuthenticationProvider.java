package com.cloudvault.security;

import com.cloudvault.domain.User;
import com.cloudvault.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;

/**
 * Database-backed authentication with real account lockout: after
 * {@link #MAX_FAILURES} consecutive bad passwords the account locks for
 * {@link #LOCK_DURATION}. Successful authentication resets the counter.
 * Registered as a bean via SecurityConfig.
 */
public class CloudVaultAuthenticationProvider extends DaoAuthenticationProvider {

    public static final int MAX_FAILURES = 10;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository users;

    public CloudVaultAuthenticationProvider(UserDetailsService userDetailsService,
                                            PasswordEncoder passwordEncoder,
                                            UserRepository users) {
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        this.users = users;
    }

    @Override
    public org.springframework.security.core.Authentication authenticate(
            org.springframework.security.core.Authentication authentication) throws AuthenticationException {
        try {
            org.springframework.security.core.Authentication result = super.authenticate(authentication);
            users.findByUsernameIgnoreCase(authentication.getName()).ifPresent(u -> {
                if (u.getFailedLoginCount() != 0 || u.getLockedUntil() != null) {
                    u.setFailedLoginCount(0);
                    u.setLockedUntil(null);
                    users.save(u);
                }
            });
            return result;
        } catch (BadCredentialsException | org.springframework.security.authentication.LockedException e) {
            recordFailure(authentication.getName());
            throw e;
        }
    }

    private void recordFailure(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(u -> {
            int failures = u.getFailedLoginCount() + 1;
            u.setFailedLoginCount(failures);
            if (failures >= MAX_FAILURES) {
                u.setLockedUntil(Instant.now().plus(LOCK_DURATION));
            }
            users.save(u);
        });
    }
}

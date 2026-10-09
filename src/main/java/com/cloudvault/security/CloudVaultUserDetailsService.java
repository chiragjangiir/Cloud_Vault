package com.cloudvault.security;

import com.cloudvault.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CloudVaultUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CloudVaultUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsernameIgnoreCase(username)
                .map(CloudVaultUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}

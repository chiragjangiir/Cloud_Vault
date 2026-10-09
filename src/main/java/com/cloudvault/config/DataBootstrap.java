package com.cloudvault.config;

import com.cloudvault.domain.Subscription;
import com.cloudvault.domain.User;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.service.StorageLocationService;
import com.cloudvault.service.StoragePoolService;
import com.cloudvault.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Startup bootstrap. Everything here operates on real infrastructure:
 * validates the configured STORAGE_ROOT, synchronizes storage pools, and
 * creates the administrator account only from environment-provided
 * credentials (never hardcoded).
 */
@Component
public class DataBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrap.class);

    private final AppProperties props;
    private final StorageLocationService locationService;
    private final StoragePoolService poolService;
    private final UserRepository users;
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrap(AppProperties props,
                         StorageLocationService locationService,
                         StoragePoolService poolService,
                         UserRepository users,
                         SubscriptionService subscriptionService,
                         PasswordEncoder passwordEncoder) {
        this.props = props;
        this.locationService = locationService;
        this.poolService = poolService;
        this.users = users;
        this.subscriptionService = subscriptionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        locationService.ensureDefaultLocation(props.getStorage().getRoot());
        poolService.ensureDefaultPool();

        String adminUser = props.getAdmin().getUsername();
        String adminPass = props.getAdmin().getPassword();
        if (adminUser != null && !adminUser.isBlank() && adminPass != null && !adminPass.isBlank()) {
            if (users.findByUsernameIgnoreCase(adminUser).isEmpty()) {
                User admin = new User();
                admin.setUsername(adminUser);
                admin.setEmail(adminUser.toLowerCase().replace(' ', '_') + "@cloudvault.local");
                admin.setPasswordHash(passwordEncoder.encode(adminPass));
                admin.setRole(User.Role.ADMIN);
                admin.setStatus(User.Status.ACTIVE);
                admin.setDisplayName("Administrator");
                admin = users.save(admin);
                subscriptionService.assignPlan(admin, "BUSINESS", Subscription.Status.ACTIVE, null, "Administrator account");
                log.info("Bootstrap administrator '{}' created from environment", adminUser);
            }
        } else {
            log.info("ADMIN_USERNAME/ADMIN_PASSWORD not set — no administrator account bootstrapped");
        }
    }
}

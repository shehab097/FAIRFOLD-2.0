package com.fairfold.config;

import com.fairfold.user.Role;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Seeds the admin account from ADMIN_EMAIL / ADMIN_PASSWORD when it does not
 * already exist. Admins never come from public registration.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final Clock clock;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, AppProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.admin().email();
        String password = properties.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        User admin = new User();
        admin.setEmail(normalized);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFirstName("Admin");
        admin.setLastName("FairFold");
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        admin.setConsentAt(clock.instant());
        userRepository.save(admin);
        log.info("Seeded admin account: {}", normalized);
    }
}

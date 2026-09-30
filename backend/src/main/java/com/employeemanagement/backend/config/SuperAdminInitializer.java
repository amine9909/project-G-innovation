package com.employeemanagement.backend.config;

import com.employeemanagement.backend.entity.User;
import com.employeemanagement.backend.enums.Permission;
import com.employeemanagement.backend.enums.UserType;
import com.employeemanagement.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashSet;

@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public SuperAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        createSuperAdminIfMissing(
                "Super Administrator 1",
                "superadmin1",
                "superadmin1@employee.local",
                "APP_ADMIN1_PASSWORD"
        );

        createSuperAdminIfMissing(
                "Super Administrator 2",
                "superadmin2",
                "superadmin2@employee.local",
                "APP_ADMIN2_PASSWORD"
        );
    }

    private void createSuperAdminIfMissing(String name, String username, String email, String passwordEnvVariable) {
        if (userRepository.findByEmailOrUsername(email, username).isPresent()) {
            return;
        }

        String rawPassword = environment.getProperty(passwordEnvVariable);
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + passwordEnvVariable);
        }

        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUserType(UserType.SUPER_ADMIN);
        user.setPermissions(new HashSet<>(EnumSet.allOf(Permission.class)));
        user.setActive(true);

        userRepository.save(user);
    }
}

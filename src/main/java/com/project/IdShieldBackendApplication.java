package com.project;

import com.project.user.entity.Role;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Main Application Entry Point for SIH26188 IDShield Backend.
 */
@Slf4j
@EnableAsync
@SpringBootApplication
public class IdShieldBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdShieldBackendApplication.class, args);
        log.info("====================================================================");
        log.info("IDShield Backend Gateway started successfully.");
        log.info("Swagger UI documentation available at: /swagger-ui.html");
        log.info("Actuator Health endpoint available at: /actuator/health");
        log.info("====================================================================");
    }

    /**
     * Development data seeder: Seeds default test users if database is empty.
     * Only runs in 'dev' profile.
     */
    @Bean
    @Profile("dev")
    public CommandLineRunner seedDemoUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@idshield.com")) {
                userRepository.save(User.builder()
                        .name("System Administrator")
                        .email("admin@idshield.com")
                        .password(passwordEncoder.encode("Admin@123456!"))
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .build());
                log.info("Seeded initial dev ADMIN user: admin@idshield.com / Admin@123456!");
            }

            if (!userRepository.existsByEmail("investigator@idshield.com")) {
                userRepository.save(User.builder()
                        .name("Lead Investigator")
                        .email("investigator@idshield.com")
                        .password(passwordEncoder.encode("Investigator@123456!"))
                        .role(Role.ROLE_INVESTIGATOR)
                        .enabled(true)
                        .build());
                log.info("Seeded initial dev INVESTIGATOR user: investigator@idshield.com / Investigator@123456!");
            }

            if (!userRepository.existsByEmail("user@idshield.com")) {
                userRepository.save(User.builder()
                        .name("Citizen User")
                        .email("user@idshield.com")
                        .password(passwordEncoder.encode("User@123456!"))
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build());
                log.info("Seeded initial dev USER: user@idshield.com / User@123456!");
            }
        };
    }
}


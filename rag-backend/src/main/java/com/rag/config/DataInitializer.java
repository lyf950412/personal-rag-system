package com.rag.config;

import com.rag.entity.User;
import com.rag.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@example.com")
                        .role("ADMIN")
                        .build();
                userRepository.save(admin);
                log.info("Created default admin user");
            }

            if (!userRepository.existsByUsername("user")) {
                User user = User.builder()
                        .username("user")
                        .password(passwordEncoder.encode("user123"))
                        .email("user@example.com")
                        .role("USER")
                        .build();
                userRepository.save(user);
                log.info("Created default test user");
            }
        };
    }
}

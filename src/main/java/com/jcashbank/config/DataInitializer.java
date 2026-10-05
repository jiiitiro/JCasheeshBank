package com.jcashbank.config;

import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User(
                        "09171234567", "1234", "Juan Dela Cruz", new BigDecimal("5000.00")));
                userRepository.save(new User(
                        "09181234567", "5678", "Maria Santos", new BigDecimal("2500.00")));
                userRepository.save(new User(
                        "09201234567", "9999", "Pedro Reyes", new BigDecimal("1000.00")));
            }
        };
    }
}

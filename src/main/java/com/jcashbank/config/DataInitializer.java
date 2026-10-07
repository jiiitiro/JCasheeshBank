package com.jcashbank.config;

import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User user1 = new User(
                        "09616049255", passwordEncoder.encode("1234"), "Julito III Tiro", new BigDecimal("5000.00"), "julitoiiitiro@gmail.com", true);

                User user2 = new User(
                        "09181234567", passwordEncoder.encode("5678"), "Maria Santos", new BigDecimal("2500.00"), "tiro.julitoiii.june091985@gmail.com", true);

                User user3 = new User(
                        "09201234567", passwordEncoder.encode("9999"), "John Wick", new BigDecimal("1000.00"), "tiro.julitoiii.06091985@gmail.com", true);

                userRepository.save(user1);
                userRepository.save(user2);
                userRepository.save(user3);
            }
        };
    }
}
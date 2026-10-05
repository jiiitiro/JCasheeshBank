package com.jcashbank.repository;

import com.jcashbank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobileNumber(String mobileNumber);
    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String token);
}

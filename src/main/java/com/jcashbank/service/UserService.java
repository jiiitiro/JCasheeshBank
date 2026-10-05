package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, JavaMailSender mailSender, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(noRollbackFor = BankingException.class)
    public User authenticate(String mobileNumber, String pin) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new BankingException("Mobile number or PIN is incorrect."));

        // Check if account is already locked
        if (user.isAccountLocked()) {
            throw new BankingException("Maximum 3 failed attempts reached. This mobile number is now locked.");
        }

        // Verify PIN using passwordEncoder
        if (!passwordEncoder.matches(pin, user.getPin())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 3) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
            }

            // Force save and commit to DB before throwing the exception
            userRepository.saveAndFlush(user);

            if (attempts >= 3) {
                throw new BankingException("Maximum 3 failed attempts reached. This mobile number is now locked.");
            }

            int remaining = 3 - attempts;
            throw new BankingException("Mobile number or PIN is incorrect. Attempts remaining: " + remaining);
        }

        // Successful login: Reset failed attempts counter
        if (user.getFailedLoginAttempts() > 0 || user.isAccountLocked()) {
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLockTime(null);
            userRepository.save(user);
        }

        return user;
    }


    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BankingException("User account was not found."));
    }

    @Transactional(readOnly = true)
    public User findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new BankingException("Receiver account was not found."));
    }

    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BankingException("Email address not found in our records."));

        // Generate a secure unique token
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Valid for 15 mins
        userRepository.save(user);

        // Send Email
        String resetLink = "http://localhost:8080/reset-pin?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("agreemo.greenhouse@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("JCasheesh! - PIN Reset Request");
        message.setText("Hi " + user.getFullName() + ",\n\nYou requested to reset your JCasheesh! PIN. Click the link below to reset it:\n" + resetLink + "\n\nThis link expires in 15 minutes.\n\nIf you didn't request this, please ignore this email.");

        mailSender.send(message);
    }

    public void resetPin(String token, String newPin) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BankingException("Invalid or expired password reset token."));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BankingException("Reset token has expired. Please request a new one.");
        }

        user.setPin(newPin);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setFailedLoginAttempts(0); // Also clear failed attempts/locks upon successful reset!
        user.setAccountLocked(false);
        userRepository.save(user);
    }




}

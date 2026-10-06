package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
//    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Validation Helper
    private void validateMobileAndPin(String mobileNumber, String pin) {
        if (mobileNumber == null || !mobileNumber.matches("^09\\d{9}$")) {
            throw new BankingException("Invalid mobile number. It must start with '09' and be exactly 11 digits.");
        }
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new BankingException("Invalid PIN. It must be exactly 4 digits.");
        }
    }

    @Transactional(noRollbackFor = BankingException.class)
    public User authenticate(String mobileNumber, String pin) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new BankingException("Mobile number or PIN is incorrect."));

        validateMobileAndPin(user.getMobileNumber(), pin);

        if (!user.isEmailVerified()) throw new BankingException("Please verify your email address first before logging in." +
                "Check your inbox for the verification link.");

        // Check if account is already locked
        if (user.isAccountLocked()) {

            // Generate a secure unique token
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Valid for 30 mins
            userRepository.saveAndFlush(user);

            String resetLink = "http://localhost:8080/reset-pin?token=" + token;
            String subject = "JCasheesh! - Mobile Number Locked & PIN Reset";
            String body = "Hi " + user.getFullName() + ",\n\n" +
                    "Your JCasheesh! mobile number has been locked due to multiple failed login attempts.\n\n" +
                    "You can reset your PIN and unlock your account by clicking the link below:\n" +
                    resetLink + "\n\n" +
                    "This link expires in 15 minutes.\n\n" +
                    "If you did not attempt to log in, please secure your account immediately.";

            emailService.sendEmailAsync(user.getEmail(), subject, body);


            throw new BankingException("Maximum 3 failed attempts reached. This mobile number is locked. Please check your email to reset your PIN.");
        }

        // Verify PIN using passwordEncoder
        if (!passwordEncoder.matches(pin, user.getPin())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 3) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());

                // Generate a secure unique token
                String token = UUID.randomUUID().toString();
                user.setResetToken(token);
                user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Valid for 30 mins
                userRepository.saveAndFlush(user);

                String resetLink = "http://localhost:8080/reset-pin?token=" + token;
                String subject = "JCasheesh! - Mobile Number Locked & PIN Reset";
                String body = "Hi " + user.getFullName() + ",\n\n" +
                        "Your JCasheesh! mobile number has been locked due to 3 failed login attempts.\n\n" +
                        "You can reset your PIN and unlock your account by clicking the link below:\n" +
                        resetLink + "\n\n" +
                        "This link expires in 15 minutes.\n\n" +
                        "If you did not attempt to log in, please secure your account immediately.";

                emailService.sendEmailAsync(user.getEmail(), subject, body);

                throw new BankingException("Maximum 3 failed attempts reached. Account locked. A reset PIN link has been sent to your email.");
            }

            userRepository.saveAndFlush(user);
            int remaining = 3 - attempts;
            throw new BankingException("Mobile number or PIN is incorrect. Attempts remaining: " + remaining);
        }

        // Successful login: Reset failed attempts counter and locks
        if (user.getFailedLoginAttempts() > 0 || user.isAccountLocked()) {
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLockTime(null);
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
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

        String resetLink = "http://localhost:8080/reset-pin?token=" + token;
        String subject = "JCasheesh! - PIN Reset Request";
        String body = "Hi " + user.getFullName() + ",\n\n" +
                "You requested to reset your JCasheesh! PIN. Click the link below to reset it:\n" +
                resetLink + "\n\n" +
                "This link expires in 15 minutes.\n\n" +
                "If you didn't request this, please ignore this email.";

        emailService.sendEmailAsync(user.getEmail(), subject, body);
    }

    @Transactional
    public void resetPin(String token, String newPin) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BankingException("Invalid or expired password reset token."));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BankingException("Reset token has expired. Please request a new one.");
        }

        // Encrypt the new PIN using BCrypt PasswordEncoder
        user.setPin(passwordEncoder.encode(newPin));

        // Clear tokens and unlock the account
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);

        userRepository.save(user);
    }

    @Transactional
    public User registerUser(User user, String rawPin) {

        validateMobileAndPin(user.getMobileNumber(), rawPin);

        user.setPin(passwordEncoder.encode(rawPin));
        user.setEmailVerified(false); // Block login initially

        // Generate verification token
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // 24-hour expiry

        User savedUser = userRepository.save(user);

        // Clean & simple call to your EmailService!
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFullName(), token);

        return savedUser;
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BankingException("Invalid or expired verification token."));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BankingException("Verification token has expired. Please register again or request a new link.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }
}

package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserValidationService userValidation;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, UserValidationService userValidation) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.userValidation = userValidation;
    }


    // Proof Ownership (Layer 3)
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

    @Transactional(noRollbackFor = BankingException.class)
    public User authenticate(String mobileNumber, String pin) {

        // 1. Check if user exists FIRST. If not, throw generic message.
        User user = userRepository.findByMobileNumber(mobileNumber.trim())
                .orElseThrow(() -> new BankingException("Mobile number or PIN is incorrect."));

        // 2. Check if this specific user account is already locked in DB
        if (user.isAccountLocked()) {
            throw new BankingException("Maximum 3 failed login attempts reached. Account locked. Please check your email to reset your PIN.");
        }

        // 3. Check email verification status
        if (!user.isEmailVerified()) {
            throw new BankingException("Please verify your email address first before logging in. Check your inbox for the verification link.");
        }

        // 4. Validate PIN match
        if (!passwordEncoder.matches(pin.trim(), user.getPin())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            // On 3rd failure for an EXISTING account, lock and send reset email
            if (attempts >= 3) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());

                String token = UUID.randomUUID().toString();
                user.setResetToken(token);
                user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
                userRepository.saveAndFlush(user);

                String resetLink = "http://localhost:8081/reset-pin?token=" + token;
                String subject = "JCasheesh! - Account Locked & PIN Reset";
                String body = "Hi " + user.getFullName() + ",\n\n" +
                        "Your JCasheesh! account has been locked due to 3 failed login attempts.\n\n" +
                        "You can reset your PIN and unlock your account by clicking the link below:\n" +
                        resetLink + "\n\n" +
                        "This link expires in 15 minutes.";

                emailService.sendEmailAsync(user.getEmail(), subject, body);

                throw new BankingException("Maximum 3 failed login attempts reached. Account locked. Please check your email to reset your PIN.");
            }

            userRepository.saveAndFlush(user);
            int remaining = 3 - attempts;
            throw new BankingException("Mobile number or PIN is incorrect. Attempts remaining: " + remaining);
        }

        // 5. Successful login: Reset DB attempt counter
        if (user.getFailedLoginAttempts() > 0) {
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

    public void processForgotPassword(String email) {

        // --- LAYER 1: Syntax Check --- EMAIL VALIDATION
        if (!userValidation.isValidEmailSyntax(email)) {
            throw new BankingException("Invalid email format syntax.");
        }
        // --- LAYER 2: MX Record Check --- EMAIL VALIDATION
        if (!userValidation.hasValidMxRecord(email)) {
            throw new BankingException("The email domain does not exist or cannot receive mail.");
        }

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

        if (passwordEncoder.matches(newPin, user.getPin())) {
            throw new BankingException("Your new PIN must be different from your old PIN.");
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

        userValidation.validateMobileAndPin(user.getMobileNumber(), rawPin);

        userValidation.validateRegistration(user.getFullName(), user.getMobileNumber(), rawPin, user.getEmail());

        // --- LAYER 1: Syntax Check --- EMAIL VALIDATION
        if (!userValidation.isValidEmailSyntax(user.getEmail())) {
            throw new BankingException("Invalid email format syntax.");
        }
        // --- LAYER 2: MX Record Check --- EMAIL VALIDATION
        if (!userValidation.hasValidMxRecord(user.getEmail())) {
            throw new BankingException("The email domain does not exist or cannot receive mail.");
        }

        user.setPin(passwordEncoder.encode(rawPin));
        user.setEmailVerified(false);

        // Generate verification token
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // 24-hour expiry

        User savedUser = userRepository.save(user);

        // --- LAYER 3: Proof of Ownership --- EMAIL VALIDATION
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFullName(), token);

        return savedUser;
    }

}

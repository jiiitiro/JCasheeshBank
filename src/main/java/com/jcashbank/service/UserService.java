package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.regex.Pattern;
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

    // Validation Methods
    private void validateMobileAndPin(String mobileNumber, String pin) {

        if (mobileNumber == null || !mobileNumber.matches("^09\\d{9}$")) {
            throw new BankingException("Invalid mobile number. It must start with '09' and be exactly 11 digits.");
        }
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new BankingException("Invalid PIN. It must be exactly 4 digits.");
        }
    }

    private void validateRegistration(String fullName, String mobileNumber, String pin, String email) {
        if (fullName == null || fullName.trim().isEmpty() || !fullName.matches("^[a-zA-Z\\s\\.\\-]+$")) {
            throw new BankingException("Invalid full name. Please use only letters, spaces, dots, or hyphens.");
        }
        if (mobileNumber == null || !mobileNumber.matches("^09\\d{9}$")) {
            throw new BankingException("Invalid mobile number. It must start with '09' and be exactly 11 digits.");
        }
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new BankingException("Invalid PIN. It must be exactly 4 digits.");
        }

        // Check if Mobile Number Already Exists
        if (userRepository.findByMobileNumber(mobileNumber).isPresent()) {
            throw new BankingException("This mobile number is already registered. Please use a different number or log in.");
        }

        // Check if Email Already Exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BankingException("This email address is already registered.");
        }
    }

    // Syntax Validation (Layer 1) ---
    private boolean isValidEmailSyntax(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email.trim());
    }

    // MX Record Domain Check (Layer 2) ---
    private boolean hasValidMxRecord(String email) {
        try {
            String domain = email.substring(email.indexOf("@") + 1);
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[] {"MX"});
            return attrs != null && attrs.get("MX") != null;
        } catch (Exception e) {
            // Domain does not exist or has no active mail server
            return false;
        }
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
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new BankingException("Mobile number or PIN is incorrect."));

        if (!user.isEmailVerified()) {
            throw new BankingException("Please verify your email address first before logging in. Check your inbox for the verification link.");
        }

        // Check if account is already locked
        if (user.isAccountLocked()) {

            // Generate a secure unique token
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.saveAndFlush(user);

            String resetLink = emailService.getBaseUrl() + "/reset-pin?token=" + token;
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

        validateMobileAndPin(user.getMobileNumber(), pin);

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
                user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
                userRepository.saveAndFlush(user);

                String resetLink = emailService.getBaseUrl() + "/reset-pin?token=" + token;
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

        // --- LAYER 1: Syntax Check --- EMAIL VALIDATION
        if (!isValidEmailSyntax(email)) {
            throw new BankingException("Invalid email format syntax.");
        }
        // --- LAYER 2: MX Record Check --- EMAIL VALIDATION
        if (!hasValidMxRecord(email)) {
            throw new BankingException("The email domain does not exist or cannot receive mail.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BankingException("Email address not found in our records."));

        // Generate a secure unique token
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Valid for 15 mins
        userRepository.save(user);

        String resetLink = emailService.getBaseUrl() + "/reset-pin?token=" + token;
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

        validateMobileAndPin(user.getMobileNumber(), rawPin);

        validateRegistration(user.getFullName(), user.getMobileNumber(), rawPin, user.getEmail());

        // --- LAYER 1: Syntax Check --- EMAIL VALIDATION
        if (!isValidEmailSyntax(user.getEmail())) {
            throw new BankingException("Invalid email format syntax.");
        }
        // --- LAYER 2: MX Record Check --- EMAIL VALIDATION
        if (!hasValidMxRecord(user.getEmail())) {
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

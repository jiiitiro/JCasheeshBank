package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.stereotype.Service;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.regex.Pattern;

@Service
public class UserValidationService {

    private final UserRepository userRepository;

    public UserValidationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateMobileAndPin(String mobileNumber, String pin) {

        if (mobileNumber == null || !mobileNumber.matches("^09\\d{9}$")) {
            throw new BankingException("Invalid mobile number. It must start with '09' and be exactly 11 digits.");
        }
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new BankingException("Invalid PIN. It must be exactly 4 digits.");
        }
    }

    public void validateRegistration(String fullName, String mobileNumber, String pin, String email) {
        if (fullName == null || fullName.trim().isEmpty() || !fullName.matches("^[a-zA-Z\\s\\.\\-]+$")) {
            throw new BankingException("Invalid full name. Please use only letters, spaces, dots, or hyphens.");
        }
        if (mobileNumber == null || !mobileNumber.matches("^09\\d{9}$")) {
            throw new BankingException("Invalid mobile number. It must start with '09' and be exactly 11 digits.");
        }
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new BankingException("Invalid PIN. It must be exactly 4 digits.");
        }

        if (userRepository.findByMobileNumber(mobileNumber).isPresent()) {
            throw new BankingException("This mobile number is already registered. Please use a different number or log in.");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BankingException("This email address is already registered.");
        }
    }

    public boolean isValidEmailSyntax(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email.trim());
    }

    // MX Record Domain Check (Layer 2) ---
    public boolean hasValidMxRecord(String email) {
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

    public String maskFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "JCasheesh User";
        }

        String[] parts = fullName.trim().split("\\s+");
        String firstName = parts[0];

        // Mask First Name (keep first 2 chars, add dots, keep last char if long enough)
        String maskedFirst;
        if (firstName.length() <= 2) {
            maskedFirst = firstName.toUpperCase() + "••";
        } else {
            maskedFirst = firstName.substring(0, 2).toUpperCase() + "••••" + firstName.substring(firstName.length() - 1).toUpperCase();
        }

        // Append Last Name Initial if available
        if (parts.length > 1) {
            String lastName = parts[parts.length - 1];
            return maskedFirst + " " + lastName.substring(0, 1).toUpperCase() + ".";
        }

        return maskedFirst;
    }

    public String getMaskedNameByMobile(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber.trim())
                .orElseThrow(() -> new BankingException("Recipient mobile number not found."));

        return maskFullName(user.getFullName());
    }

}

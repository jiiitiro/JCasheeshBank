package com.jcashbank.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async // 👈 Runs this entire method in a background thread pool
    public void sendEmailAsync(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("agreemo.greenhouse@gmail.com");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("SUCCESS: Email sent asynchronously to " + toEmail);
        } catch (Exception e) {
            System.err.println("FAILED TO SEND EMAIL: " + e.getMessage());
        }
    }

    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                String verifyLink = "http://localhost:8080/verify?token=" + token;
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("agreemo.greenhouse@gmail.com");
                message.setTo(toEmail);
                message.setSubject("JCasheesh! - Verify Your Email Address");
                message.setText("Hi " + fullName + ",\n\n" +
                        "Thank you for registering with JCasheesh! Please click the link below to verify your email address and activate your account:\n" +
                        verifyLink + "\n\n" +
                        "This link expires in 24 hours.\n\n" +
                        "If you did not create an account, please ignore this email.");

                mailSender.send(message);
                System.out.println("SUCCESS: Verification email sent to " + toEmail);
            } catch (Exception e) {
                System.err.println("FAILED TO SEND VERIFICATION EMAIL: " + e.getMessage());
            }
        });
    }
}

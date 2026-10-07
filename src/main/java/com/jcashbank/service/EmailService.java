package com.jcashbank.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    @Value("${APP_BASE_URL}")
    private String baseUrl;

    @Async // 👈 Runs this entire method in a background thread pool
    public void sendEmailAsync(String toEmail, String subject, String body) {
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("JCasheeshBank <onboarding@resend.dev>") // Required for free tier testing
                    .to(toEmail)
                    .subject(subject)
                    .html("<p>" + body.replace("\n", "<br>") + "</p>")
                    .build();

            resend.emails().send(params);
            System.out.println("SUCCESS: Email sent asynchronously to " + toEmail);
        } catch (Exception e) {
            System.err.println("FAILED TO SEND EMAIL: " + e.getMessage());
        }
    }

    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                // Dynamically uses localhost for development and Render URL for production
                String verifyLink = baseUrl + "/verify?token=" + token;
                Resend resend = new Resend(apiKey);

                String htmlContent = "Hi " + fullName + ",<br><br>" +
                        "Thank you for registering with JCasheesh! Please click the link below to verify your email address and activate your account:<br>" +
                        "<a href=\"" + verifyLink + "\">Verify Account</a><br><br>" +
                        "This link expires in 24 hours.<br><br>" +
                        "If you did not create an account, please ignore this email.";

                CreateEmailOptions params = CreateEmailOptions.builder()
                        .from("JCasheeshBank <onboarding@resend.dev>")
                        .to(toEmail)
                        .subject("JCasheesh! - Verify Your Email Address")
                        .html(htmlContent)
                        .build();

                resend.emails().send(params);
                System.out.println("SUCCESS: Verification email sent to " + toEmail);
            } catch (Exception e) {
                System.err.println("FAILED TO SEND VERIFICATION EMAIL: " + e.getMessage());
            }
        });
    }
}
package com.jcashbank.service;

// import com.resend.Resend;
// import com.resend.services.emails.model.CreateEmailOptions;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Commented out Resend API key injection
    // @Value("${resend.api.key:-dummy_key_for_dev}")
    // private String apiKey;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${spring.mail.username:your-email@gmail.com}")
    private String fromEmail;

    // Explicit Constructor Injection (No Lombok @RequiredArgsConstructor)
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Async // Runs asynchronously in background thread pool
    public void sendEmailAsync(String toEmail, String subject, String body) {
        // === Gmail SMTP Implementation ===
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText("<p>" + body.replace("\n", "<br>") + "</p>", true);

            mailSender.send(message);
            System.out.println("SUCCESS: Email sent asynchronously to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("FAILED TO SEND EMAIL via Gmail SMTP: " + e.getMessage());
        }

        /* === Commented Out: Resend API Implementation ===
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("JCasheeshBank <onboarding@resend.dev>")
                    .to(toEmail)
                    .subject(subject)
                    .html("<p>" + body.replace("\n", "<br>") + "</p>")
                    .build();

            resend.emails().send(params);
            System.out.println("SUCCESS: Email sent asynchronously to " + toEmail);
        } catch (Exception e) {
            System.err.println("FAILED TO SEND EMAIL: " + e.getMessage());
        }
        */
    }

    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        String verifyLink = baseUrl + "/verify?token=" + token;

        String htmlContent = "Hi " + fullName + ",<br><br>" +
                "Thank you for registering with JCasheesh! Please click the link below to verify your email address and activate your account:<br>" +
                "<a href=\"" + verifyLink + "\">Verify Account</a><br><br>" +
                "This link expires in 24 hours.<br><br>" +
                "If you did not create an account, please ignore this email.";

        // === Gmail SMTP Implementation ===
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("JCasheesh! - Verify Your Email Address");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("SUCCESS: Verification email sent via Gmail SMTP to " + toEmail);
        } catch (MessagingException e) {
            System.err.println("FAILED TO SEND VERIFICATION EMAIL via Gmail SMTP: " + e.getMessage());
        }

        /* === Commented Out: Resend API Implementation ===
        try {
            Resend resend = new Resend(apiKey);

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
        */
    }
}

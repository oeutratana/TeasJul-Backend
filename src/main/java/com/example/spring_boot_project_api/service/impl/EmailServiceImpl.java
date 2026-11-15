package com.example.spring_boot_project_api.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.exception.EmailDeliveryException;
import com.example.spring_boot_project_api.service.EmailService;

/**
 * Email provider built on Spring Boot's mail infrastructure. All SMTP
 * credentials and the from-address are read from configuration/environment
 * variables and are never hardcoded.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final String APP_NAME = "TeasJul Rental Management System";

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean mailEnabled;

    public EmailServiceImpl(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.mailEnabled = mailEnabled;
    }

    @Override
    public void sendRecoveryEmailVerificationOtp(String to, String otp, int expiresInMinutes) {
        send(to, "Verify your recovery email",
                buildOtpBody(otp, expiresInMinutes,
                        "You are adding this email as the recovery email for your " + APP_NAME
                                + " account."));
    }

    @Override
    public void sendPasswordResetOtp(String to, String otp, int expiresInMinutes) {
        send(to, "Password reset verification code",
                buildOtpBody(otp, expiresInMinutes,
                        "You (or someone on your behalf) requested to reset the password for your "
                                + APP_NAME + " account."));
    }

    @Override
    public void sendTwoFactorOtp(String to, String otp, int expiresInMinutes) {
        send(to, "Your sign-in verification code",
                buildOtpBody(otp, expiresInMinutes,
                        "A sign-in attempt was made to your " + APP_NAME + " account."));
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            // In development, silently discard emails; the OTP is surfaced in logs
            // for the developer only, never to end users.
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Mail delivery is disabled (app.mail.enabled=false). "
                            + "Would have sent '{}' to {}", subject, to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Do not leak the OTP or SMTP credentials in the log; surface a generic
            // message so the request fails with a clear 4xx instead of a 500.
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Failed to deliver email '{}' to {}: {}", subject, to, ex.getMessage());
            throw new EmailDeliveryException(
                    "We could not send the verification email. Please try again later.");
        }
    }

    private String buildOtpBody(String otp, int expiresInMinutes, String contextLine) {
        return """
                  %s

                  %s

                  Your verification code is:

                  %s

                  This code expires in %d minutes.

                  If you did not request this, please ignore this email and contact your administrator.
                  Do not share this code with anyone. %s will never ask you for it.

                  — %s
                """.formatted(APP_NAME, contextLine, otp, expiresInMinutes, APP_NAME, APP_NAME);
    }
}

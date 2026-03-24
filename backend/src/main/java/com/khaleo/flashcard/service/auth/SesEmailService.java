package com.khaleo.flashcard.service.auth;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
@Service
@Profile("production")
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "ses")
@RequiredArgsConstructor
public class SesEmailService implements EmailService {

    private final SesClient sesClient;
    private final AuthAuditLogger authAuditLogger;

    @Value("${app.auth.email.from}")
    private String fromAddress;

    @Value("${app.auth.email.verification-base-url}")
    private String verificationBaseUrl;

    @Value("${app.auth.email.reset-base-url}")
    private String resetBaseUrl;

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationUrl = verificationBaseUrl + "?token=" + verificationToken;
        String html = "<p>Please verify your account.</p><p><a href=\"" + verificationUrl + "\">Verify email</a></p>";

        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromAddress)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().data("Verify your Kha Leo account").build())
                        .body(Body.builder().html(Content.builder().data(html).build()).build())
                        .build())
                .build();

        try {
            sesClient.sendEmail(request);
            authAuditLogger.logEvent("auth_verification_email_sent", Map.of("email", toEmail));
            log.info("event=auth_verification_email_sent email={}", toEmail);
        } catch (Exception e) {
            log.error("event=auth_verification_email_failed email={} error={}", toEmail, e.getMessage(), e);
            authAuditLogger.logEvent("auth_verification_email_failed", Map.of("email", toEmail, "error", e.getMessage()));
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = resetBaseUrl + "?token=" + resetToken;
        String html = "<p>Password reset requested.</p><p><a href=\"" + resetUrl + "\">Reset password</a></p>";

        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromAddress)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().data("Reset your Kha Leo password").build())
                        .body(Body.builder().html(Content.builder().data(html).build()).build())
                        .build())
                .build();

        try {
            sesClient.sendEmail(request);
            authAuditLogger.logEvent("auth_password_reset_email_sent", Map.of("email", toEmail));
            log.info("event=auth_password_reset_email_sent email={}", toEmail);
        } catch (Exception e) {
            log.error("event=auth_password_reset_email_failed email={} error={}", toEmail, e.getMessage(), e);
            authAuditLogger.logEvent("auth_password_reset_email_failed", Map.of("email", toEmail, "error", e.getMessage()));
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
}

package com.khaleo.flashcard.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock email service used for local development.
 * Logs emails instead of sending them via AWS SES.
 * 
 * Active when:
 * - Spring profile is NOT "production"
 * - AND app.email.provider is "mock" or not set (default)
 */
@Slf4j
@Service
@Profile("!production")
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService {

    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationUrl = "http://localhost:5173/verify?token=" + verificationToken;
        log.info("🔷 [MOCK EMAIL] Verification email to: {} with token: {}", toEmail, verificationToken);
        log.info("🔷 [MOCK EMAIL] Verification URL: {}", verificationUrl);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = "http://localhost:5173/reset-password?token=" + resetToken;
        log.info("🔷 [MOCK EMAIL] Password reset email to: {} with token: {}", toEmail, resetToken);
        log.info("🔷 [MOCK EMAIL] Reset URL: {}", resetUrl);
    }
}

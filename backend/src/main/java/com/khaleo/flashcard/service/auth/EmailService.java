package com.khaleo.flashcard.service.auth;

/**
 * Email service abstraction for sending verification and password reset emails.
 * Implementations vary by environment (mock for local dev, AWS SES for production).
 */
public interface EmailService {
    /**
     * Send verification email with token link.
     * @param toEmail recipient email address
     * @param verificationToken token for email verification
     */
    void sendVerificationEmail(String toEmail, String verificationToken);

    /**
     * Send password reset email with token link.
     * @param toEmail recipient email address
     * @param resetToken token for password reset
     */
    void sendPasswordResetEmail(String toEmail, String resetToken);
}

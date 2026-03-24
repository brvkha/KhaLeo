package com.khaleo.flashcard.service.auth;

import com.khaleo.flashcard.entity.EmailVerificationToken;
import com.khaleo.flashcard.entity.User;
import com.khaleo.flashcard.entity.enums.UserRole;
import com.khaleo.flashcard.repository.EmailVerificationTokenRepository;
import com.khaleo.flashcard.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthAuditLogger authAuditLogger;

    @Value("${app.auth.email.verification-required:true}")
    private boolean emailVerificationRequired;

    public RegisterResult registerGuest(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthDomainException(HttpStatus.CONFLICT, AuthErrorCode.DUPLICATE_EMAIL, "Email already exists.");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserRole.ROLE_USER)
                .isEmailVerified(!emailVerificationRequired)  // Auto-verify if verification not required (local dev)
                .failedLoginAttempts(0)
                .build();

        User savedUser = userRepository.save(user);

        if (emailVerificationRequired) {
            String verificationToken = UUID.randomUUID().toString();
            EmailVerificationToken token = EmailVerificationToken.builder()
                .token(verificationToken)
                .user(savedUser)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
            emailVerificationTokenRepository.save(token);
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        }
        authAuditLogger.logEvent(
                "auth_registration_completed",
                Map.of("userId", savedUser.getId(), "email", savedUser.getEmail()));

        return new RegisterResult(savedUser.getId(), savedUser.getEmail(), emailVerificationRequired);
    }

    public void verifyEmailToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Verification token is required.");
        }

        EmailVerificationToken token = emailVerificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new AuthDomainException(
                        HttpStatus.NOT_FOUND,
                        AuthErrorCode.INVALID_VERIFICATION_TOKEN,
                        "Verification token was not found."));

        if (token.isConsumed()) {
            throw new AuthDomainException(
                    HttpStatus.NOT_FOUND,
                    AuthErrorCode.INVALID_VERIFICATION_TOKEN,
                    "Verification token is no longer valid.");
        }

        if (token.isExpiredAt(Instant.now())) {
            throw new AuthDomainException(
                    HttpStatus.GONE,
                    AuthErrorCode.EXPIRED_VERIFICATION_TOKEN,
                    "Verification token has expired.");
        }

        User user = token.getUser();
        user.setIsEmailVerified(Boolean.TRUE);
        userRepository.save(user);

        token.setConsumedAt(Instant.now());
        emailVerificationTokenRepository.save(token);

        authAuditLogger.logEvent("auth_verification_completed", Map.of("userId", user.getId(), "email", user.getEmail()));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record RegisterResult(UUID userId, String email, boolean verificationRequired) {
    }
}

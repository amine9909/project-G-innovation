package com.employeemanagement.backend.service;

import com.employeemanagement.backend.dto.auth.ForgotPasswordRequest;
import com.employeemanagement.backend.dto.auth.PasswordResetMessageResponse;
import com.employeemanagement.backend.dto.auth.ResetPasswordRequest;
import com.employeemanagement.backend.entity.PasswordResetToken;
import com.employeemanagement.backend.entity.User;
import com.employeemanagement.backend.repository.PasswordResetTokenRepository;
import com.employeemanagement.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final String NEUTRAL_MESSAGE =
            "If an account exists for this email, a password reset link has been sent.";
    private static final String RESET_SUCCESS_MESSAGE = "Your password has been reset successfully.";
    private static final String INVALID_TOKEN_MESSAGE = "The password reset link is invalid or has expired.";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;
    private final long expirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend.url}") String frontendUrl,
            @Value("${app.password-reset.expiration-ms}") long expirationMs) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public PasswordResetMessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> {
                    passwordResetTokenRepository.markUnusedTokensAsUsedForUser(user);

                    String rawToken = generateRawToken();
                    PasswordResetToken resetToken = new PasswordResetToken(
                            user,
                            hashToken(rawToken),
                            Instant.now().plusMillis(expirationMs),
                            false,
                            Instant.now()
                    );

                    passwordResetTokenRepository.save(resetToken);
                    emailService.sendPasswordResetEmail(user, buildResetLink(rawToken));

                    return neutralResponse();
                })
                .orElseGet(this::neutralResponse);
    }

    @Transactional
    public PasswordResetMessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password confirmation does not match.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(request.getToken()))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN_MESSAGE));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
        }

        User user = resetToken.getUser();

        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        resetToken.setUsed(true);
        passwordResetTokenRepository.markUnusedTokensAsUsedForUser(user);
        userRepository.save(user);

        return new PasswordResetMessageResponse(RESET_SUCCESS_MESSAGE);
    }

    private PasswordResetMessageResponse neutralResponse() {
        return new PasswordResetMessageResponse(NEUTRAL_MESSAGE);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Password reset token hashing is unavailable", exception);
        }
    }

    private String buildResetLink(String rawToken) {
        String baseUrl = frontendUrl.replaceAll("/+$", "");
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        return baseUrl + "/reset-password?token=" + encodedToken;
    }
}

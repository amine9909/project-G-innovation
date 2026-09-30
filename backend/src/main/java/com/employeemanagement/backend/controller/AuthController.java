package com.employeemanagement.backend.controller;

import com.employeemanagement.backend.dto.auth.LoginRequest;
import com.employeemanagement.backend.dto.auth.LoginResponse;
import com.employeemanagement.backend.dto.auth.ForgotPasswordRequest;
import com.employeemanagement.backend.dto.auth.PasswordResetMessageResponse;
import com.employeemanagement.backend.dto.auth.ResetPasswordRequest;
import com.employeemanagement.backend.service.AuthService;
import com.employeemanagement.backend.service.AuthService.LoginResult;
import com.employeemanagement.backend.service.AuthService.RefreshResult;
import com.employeemanagement.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final long accessExpirationMs;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            PasswordResetService passwordResetService,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.cookie-secure:false}") boolean secureCookie) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.accessExpirationMs = accessExpirationMs;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult loginResult = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createCookie(
                        ACCESS_TOKEN_COOKIE_NAME,
                        loginResult.accessToken(),
                        accessCookieMaxAge(loginResult.sessionExpiresAt())
                ).toString())
                .header(HttpHeaders.SET_COOKIE, createCookie(
                        REFRESH_TOKEN_COOKIE_NAME,
                        loginResult.refreshToken(),
                        refreshCookieMaxAge(loginResult.sessionExpiresAt())
                ).toString())
                .body(loginResult.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        try {
            RefreshResult refreshResult = authService.refresh(refreshToken);

            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, createCookie(
                            ACCESS_TOKEN_COOKIE_NAME,
                            refreshResult.accessToken(),
                            accessCookieMaxAge(refreshResult.sessionExpiresAt())
                    ).toString())
                    .header(HttpHeaders.SET_COOKIE, createCookie(
                            REFRESH_TOKEN_COOKIE_NAME,
                            refreshResult.refreshToken(),
                            refreshCookieMaxAge(refreshResult.sessionExpiresAt())
                    ).toString())
                    .build();
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_TOKEN_COOKIE_NAME).toString())
                    .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                    .build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_TOKEN_COOKIE_NAME).toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetMessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetMessageResponse response = passwordResetService.resetPassword(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_TOKEN_COOKIE_NAME).toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .body(response);
    }

    private ResponseCookie createCookie(String name, String token, long expirationMs) {
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
    }

    private long accessCookieMaxAge(long sessionExpiresAt) {
        return Math.min(accessExpirationMs, refreshCookieMaxAge(sessionExpiresAt));
    }

    private long refreshCookieMaxAge(long sessionExpiresAt) {
        return Math.max(0, sessionExpiresAt - System.currentTimeMillis());
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }
}

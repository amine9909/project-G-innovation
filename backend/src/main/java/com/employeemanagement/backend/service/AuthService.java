package com.employeemanagement.backend.service;

import com.employeemanagement.backend.dto.auth.LoginRequest;
import com.employeemanagement.backend.dto.auth.LoginResponse;
import com.employeemanagement.backend.entity.User;
import com.employeemanagement.backend.repository.UserRepository;
import com.employeemanagement.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailOrUsername(
                        request.getIdentifier(),
                        request.getIdentifier()
                )
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email or username: " + request.getIdentifier()
        ));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        long sessionExpiresAt = jwtService.createSessionExpiresAt();
        String accessToken = jwtService.generateAccessToken(userDetails.getUsername(), sessionExpiresAt);
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername(), sessionExpiresAt);

        LoginResponse response = new LoginResponse(
                        user.getId(),
                        user.getName(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getUserType(),
                        new HashSet<>(user.getPermissions())
        );

        return new LoginResult(response, accessToken, refreshToken, sessionExpiresAt);
    }

    public RefreshResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        long sessionExpiresAt = jwtService.extractSessionExpiresAt(refreshToken);

        if (System.currentTimeMillis() >= sessionExpiresAt) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        return new RefreshResult(
                jwtService.generateAccessToken(user.getUsername(), sessionExpiresAt),
                jwtService.generateRefreshToken(user.getUsername(), sessionExpiresAt),
                sessionExpiresAt
        );
    }

    public record LoginResult(LoginResponse response, String accessToken, String refreshToken, long sessionExpiresAt) {
    }

    public record RefreshResult(String accessToken, String refreshToken, long sessionExpiresAt) {
    }
}

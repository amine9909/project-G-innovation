package com.employeemanagement.backend.service;

import com.employeemanagement.backend.dto.user.CreateUserRequest;
import com.employeemanagement.backend.dto.user.UpdatePermissionsRequest;
import com.employeemanagement.backend.dto.user.UpdateUserRequest;
import com.employeemanagement.backend.dto.user.UserResponse;
import com.employeemanagement.backend.entity.User;
import com.employeemanagement.backend.enums.UserType;
import com.employeemanagement.backend.repository.PasswordResetTokenRepository;
import com.employeemanagement.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(UserType.NORMAL_USER);
        user.setPermissions(new HashSet<>());
        user.setActive(true);

        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String search, Pageable pageable) {
        Page<User> users;

        if (search == null || search.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(
                    search,
                    search,
                    search,
                    pageable
            );
        }

        return users.map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + currentUsername));

        if (user.getUserType() == UserType.SUPER_ADMIN && currentUser.getUserType() != UserType.SUPER_ADMIN) {
            throw new AccessDeniedException("You are not allowed to modify a Super Admin account.");
        }

        userRepository.findByEmail(request.getEmail())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Email is already in use");
                });

        userRepository.findByUsername(request.getUsername())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Username is already in use");
                });

        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setActive(request.getActive());

        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (user.getUserType() == UserType.SUPER_ADMIN) {
            throw new IllegalStateException("SUPER_ADMIN accounts cannot be deleted");
        }

        passwordResetTokenRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse updatePermissions(Long id, UpdatePermissionsRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (user.getUserType() == UserType.SUPER_ADMIN) {
            throw new IllegalStateException("SUPER_ADMIN permissions cannot be modified");
        }

        user.setPermissions(new HashSet<>(request.getPermissions()));

        return toUserResponse(userRepository.save(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getUserType(),
                new HashSet<>(user.getPermissions()),
                user.isActive()
        );
    }
}

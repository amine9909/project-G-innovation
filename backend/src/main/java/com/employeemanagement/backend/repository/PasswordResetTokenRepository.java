package com.employeemanagement.backend.repository;

import com.employeemanagement.backend.entity.PasswordResetToken;
import com.employeemanagement.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(User user);

    @Modifying
    @Query("update PasswordResetToken token set token.used = true where token.user = :user and token.used = false")
    int markUnusedTokensAsUsedForUser(@Param("user") User user);
}

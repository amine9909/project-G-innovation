package com.employeemanagement.backend.service;

import com.employeemanagement.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordResetEmail(User user, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Reset your Employee Management password");
        message.setText("""
                Hello %s,

                We received a request to reset your password.

                Use the link below to choose a new password:

                %s

                This link expires in 30 minutes.

                If you did not request a password reset, you can ignore this email.
                """.formatted(user.getName(), resetLink));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.error("Failed to send password reset email for user id: {}", user.getId(), exception);
            throw new IllegalStateException("Unable to send password reset email", exception);
        }
    }
}

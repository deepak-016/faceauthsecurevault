package com.faceauth.faceauth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");

    // @Async: runs on a separate thread so a slow/unreachable SMTP server
    // can never block the HTTP request (e.g. login) that triggered it.
    @Async
    public void sendLoginAlert(String toEmail, String username) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            String time = LocalDateTime.now().format(TIME_FORMAT);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("FaceAuthWeb — New Login Detected");
            message.setText("Hi " + username + ",\n\nWe noticed a new login to your FaceAuthWeb account.\n\nTime: " + time + "\n\nIf this was you, no action is needed.\nIf you did not log in, please secure your account immediately.\n\n— FaceAuthWeb Team");
            mailSender.send(message);
            System.out.println("[EmailService] Login alert email sent to " + toEmail);
        } catch (Exception e) {
            System.out.println("[EmailService] Failed to send login alert: " + e.getMessage());
        }
    }

    @Async
    public void sendUploadAlert(String toEmail, String username, String fileName) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            String time = LocalDateTime.now().format(TIME_FORMAT);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("FaceAuthWeb — File Upload Confirmation");
            message.setText("Hi " + username + ",\n\nA new file was uploaded to your FaceAuthWeb account.\n\nFile name: " + fileName + "\nTime: " + time + "\n\nIf you did not perform this upload, please secure your account immediately.\n\n— FaceAuthWeb Team");
            mailSender.send(message);
            System.out.println("[EmailService] Upload alert email sent to " + toEmail);
        } catch (Exception e) {
            System.out.println("[EmailService] Failed to send upload alert: " + e.getMessage());
        }
    }
}

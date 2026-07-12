package com.faceauth.faceauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EmailService — sends notification emails via the Brevo HTTPS API
 * (https://api.brevo.com), NOT raw SMTP.
 *
 * Why: Railway (and many free-tier hosts) block outbound SMTP on all ports
 * (25 / 465 / 587) to prevent spam abuse. Regular HTTPS (port 443) is never
 * blocked, so sending through Brevo's REST API works reliably in
 * production, while a local JavaMailSender/SMTP setup would time out.
 *
 * Setup required (see README_EMAIL_SETUP.md):
 *   1. Free Brevo account -> generate an API key
 *   2. Verify a sender email in Brevo
 *   3. Set BREVO_API_KEY and BREVO_SENDER_EMAIL as environment variables
 */
@Service
public class EmailService {

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:}")
    private String senderEmail;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Sent on a plain background thread so a slow/failed email never blocks
    // the login or upload request itself.
    public void sendLoginAlert(String toEmail, String username) {
        if (toEmail == null || toEmail.isBlank()) return;
        Thread t = new Thread(() -> {
            String time = LocalDateTime.now().format(TIME_FORMAT);
            String subject = "FaceAuthWeb — New Login Detected";
            String body = "Hi " + username + ",<br><br>We noticed a new login to your FaceAuthWeb account.<br><br>Time: "
                    + time + "<br><br>If this was you, no action is needed.<br>If you did not log in, please secure your account immediately.<br><br>— FaceAuthWeb Team";
            send(toEmail, subject, body, "login");
        }, "email-login-alert");
        t.setDaemon(true);
        t.start();
    }

    public void sendUploadAlert(String toEmail, String username, String fileName) {
        if (toEmail == null || toEmail.isBlank()) return;
        Thread t = new Thread(() -> {
            String time = LocalDateTime.now().format(TIME_FORMAT);
            String subject = "FaceAuthWeb — File Upload Confirmation";
            String body = "Hi " + username + ",<br><br>A new file was uploaded to your FaceAuthWeb account.<br><br>File name: "
                    + fileName + "<br>Time: " + time + "<br><br>If you did not perform this upload, please secure your account immediately.<br><br>— FaceAuthWeb Team";
            send(toEmail, subject, body, "upload");
        }, "email-upload-alert");
        t.setDaemon(true);
        t.start();
    }

    private void send(String toEmail, String subject, String htmlBody, String kind) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.out.println("[EmailService] Skipped " + kind + " email — BREVO_API_KEY not set.");
            return;
        }
        try {
            String json = "{"
                    + "\"sender\":{\"email\":\"" + senderEmail + "\"},"
                    + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
                    + "\"subject\":\"" + subject + "\","
                    + "\"htmlContent\":\"" + htmlBody + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[EmailService] " + kind + " alert email sent to " + toEmail);
            } else {
                System.out.println("[EmailService] Failed to send " + kind + " email. Status: "
                        + response.statusCode() + " Body: " + response.body());
            }
        } catch (Exception e) {
            System.out.println("[EmailService] Exception sending " + kind + " email: " + e.getMessage());
        }
    }
}

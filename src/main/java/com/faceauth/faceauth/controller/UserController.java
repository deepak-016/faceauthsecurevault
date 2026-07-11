package com.faceauth.faceauth.controller;

import com.faceauth.faceauth.entity.User;
import com.faceauth.faceauth.repository.FaceEmbeddingRepository;
import com.faceauth.faceauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * UserController — provides the endpoints the dashboard frontend calls
 * that were missing from the original codebase.
 *
 * All routes are under /api/user  +  /api/face
 *
 * Token format: "token-{userId}-{timestamp}"  (matches AuthController output)
 * A real app should use JWT; for now we parse the userId from the token string.
 */
@RestController
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired UserRepository userRepo;
    @Autowired FaceEmbeddingRepository embeddingRepo;

    // ── Helper: extract userId from token header ──────────────────────────
    private Long userIdFromRequest(jakarta.servlet.http.HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7); // "token-{id}-{ts}" OR "face-token-{id}-{ts}"
        try {
            // strip leading non-digit prefix ("token-" or "face-token-")
            String[] parts = token.split("-");
            // last two parts are id and timestamp; id is the second-to-last
            return Long.parseLong(parts[parts.length - 2]);
        } catch (Exception e) {
            return null;
        }
    }

    // ── GET /api/face/status ──────────────────────────────────────────────
    @GetMapping("/api/face/status")
    public ResponseEntity<?> faceStatus(jakarta.servlet.http.HttpServletRequest req) {
        Long userId = userIdFromRequest(req);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        boolean enrolled = embeddingRepo.findByUserId(userId).isPresent();
        return ResponseEntity.ok(Map.of("enrolled", enrolled));
    }

    // ── PUT /api/user/profile ─────────────────────────────────────────────
    @PutMapping("/api/user/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest req) {

        Long userId = userIdFromRequest(req);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<User> opt = userRepo.findById(userId);
        if (opt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));

        User user = opt.get();
        String fullName = body.get("fullName");
        String email    = body.get("email");

        if (fullName != null && !fullName.isBlank()) user.setName(fullName);
        if (email    != null && !email.isBlank())    user.setEmail(email);
        userRepo.save(user);

        return ResponseEntity.ok(Map.of(
            "message",  "Profile updated",
            "fullName", user.getName(),
            "email",    user.getEmail()
        ));
    }

    // ── PUT /api/user/change-password ─────────────────────────────────────
    @PutMapping("/api/user/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest req) {

        Long userId = userIdFromRequest(req);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<User> opt = userRepo.findById(userId);
        if (opt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));

        User user = opt.get();
        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (!user.getPassword().equals(currentPassword))
            return ResponseEntity.status(401).body(Map.of("message", "Current password is incorrect"));

        if (newPassword == null || newPassword.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 8 characters"));

        user.setPassword(newPassword);
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ── DELETE /api/face/delete ───────────────────────────────────────────
    @DeleteMapping("/api/face/delete")
    public ResponseEntity<?> deleteFace(jakarta.servlet.http.HttpServletRequest req) {
        Long userId = userIdFromRequest(req);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        return embeddingRepo.findByUserId(userId).map(fe -> {
            embeddingRepo.delete(fe);
            return ResponseEntity.ok(Map.of("message", "Face data deleted"));
        }).orElse(
            ResponseEntity.status(404).body(Map.of("message", "No face data found"))
        );
    }

    // ── GET /api/dashboard/stats ──────────────────────────────────────────
    // Returns placeholder stats (you can wire up a LoginLog table later)
    @GetMapping("/api/dashboard/stats")
    public ResponseEntity<?> dashboardStats(jakarta.servlet.http.HttpServletRequest req) {
        Long userId = userIdFromRequest(req);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        boolean hasFace = embeddingRepo.findByUserId(userId).isPresent();
        return ResponseEntity.ok(Map.of(
            "totalLogins",        0,
            "faceVerifications",  0,
            "failedAttempts",     0,
            "securityScore",      hasFace ? 80 : 40,
            "loginsThisWeek",     0,
            "faceLoginsThisWeek", 0,
            "failedThisWeek",     0,
            "scoreChange",        0
        ));
    }

    // ── GET /api/dashboard/activity ───────────────────────────────────────
    // Returns empty list until you add a LoginLog entity/table
    @GetMapping("/api/dashboard/activity")
    public ResponseEntity<?> dashboardActivity(jakarta.servlet.http.HttpServletRequest req) {
        Long userId = userIdFromRequest(req);
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        // Return an empty list for now — wire up a LoginLog table to populate this
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }
}

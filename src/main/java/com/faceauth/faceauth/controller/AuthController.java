package com.faceauth.faceauth.controller;
import com.faceauth.faceauth.service.EmailService;
import com.faceauth.faceauth.entity.User;
import com.faceauth.faceauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    UserRepository userRepo;
    @Autowired
    EmailService emailService;
    // ── REGISTER ──────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {
        String fullName  = req.get("fullName");
        String username  = req.get("username");
        String email     = req.get("email");
        String password  = req.get("password");
        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "All fields are required"));
        }
        // Check if name already exists
        if (userRepo.findByName(username).isPresent()) {
            return ResponseEntity.status(409)
                .body(Map.of("message", "Username already exists"));
        }
        User user = new User();
        user.setName(username);
        user.setEmail(email);
        user.setPassword(password); // plain for now
        userRepo.save(user);
        // Return token + user object
        String token = "token-" + user.getId() + "-" + System.currentTimeMillis();
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of(
                "id",       user.getId(),
                "username", user.getName(),
                "fullName", fullName != null ? fullName : username,
                "email",    user.getEmail()
            )
        ));
    }
    // ── LOGIN ─────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        Optional<User> userOpt = userRepo.findByName(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401)
                .body(Map.of("message", "User not found"));
        }
        User user = userOpt.get();
        if (!user.getPassword().equals(password)) {
            return ResponseEntity.status(401)
                .body(Map.of("message", "Invalid password"));
        }
        String token = "token-" + user.getId() + "-" + System.currentTimeMillis();

        // ── Send login notification email (does not block login if it fails) ──
        try {
            emailService.sendLoginAlert(user.getEmail(), user.getName());
        } catch (Exception e) {
            System.out.println("[AuthController] Failed to send login email: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of(
                "id",       user.getId(),
                "username", user.getName(),
                "fullName", user.getName(),
                "email",    user.getEmail()
            )
        ));
    }
}
package com.faceauth.faceauth.controller;

import com.faceauth.faceauth.entity.FaceEmbedding;
import com.faceauth.faceauth.entity.FaceRequest;
import com.faceauth.faceauth.entity.User;
import com.faceauth.faceauth.repository.FaceEmbeddingRepository;
import com.faceauth.faceauth.repository.UserRepository;
import com.faceauth.faceauth.service.FaceService;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/face")
@CrossOrigin(origins = "*")
public class FaceAuthController {

    @Autowired FaceService faceService;
    @Autowired FaceEmbeddingRepository embeddingRepo;
    @Autowired UserRepository userRepo;

    // POST /api/auth/face/enroll
    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody FaceRequest req) {

        // looks up user by name field
        User user = userRepo.findByName(req.getUsername())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        Mat image = faceService.base64ToMat(req.getImageBase64());
        Mat face  = faceService.detectFace(image);

        if (face == null)
            return ResponseEntity.badRequest()
                .body(Map.of("message", "No face detected. Please retake photo."));

        try {
            float[] embedding = faceService.extractEmbedding(face);
            byte[] blob       = faceService.serializeEmbedding(embedding);

            FaceEmbedding fe = embeddingRepo.findByUserId(user.getId())
                .orElse(new FaceEmbedding());
            fe.setUserId(user.getId());
            fe.setEmbedding(blob);
            embeddingRepo.save(fe);

            return ResponseEntity.ok(Map.of("message", "Face enrolled successfully"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Enrollment failed: " + e.getMessage()));
        }
    }

    // POST /api/auth/face/verify
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody FaceRequest req) {

        User user = userRepo.findByName(req.getUsername())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        FaceEmbedding stored = embeddingRepo.findByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Face not enrolled for this user"));

        Mat image = faceService.base64ToMat(req.getImageBase64());
        Mat face  = faceService.detectFace(image);

        if (face == null)
            return ResponseEntity.badRequest()
                .body(Map.of("message", "No face detected in frame"));

        try {
            float[] incoming  = faceService.extractEmbedding(face);
            float[] storedEmb = faceService.deserializeEmbedding(stored.getEmbedding());
            double confidence = faceService.cosineSimilarity(incoming, storedEmb);
            boolean verified  = confidence >= 0.60;

            if (verified) {
                String token = "face-token-" + user.getId() + "-" + System.currentTimeMillis();

                return ResponseEntity.ok(Map.of(
                    "verified",   true,
                    "confidence", confidence,
                    "token",      token,
                    "user", Map.of(
                        "id",       user.getId(),
                        "username", user.getName(),   // name field from User.java
                        "fullName", user.getName(),   // same name field
                        "email",    user.getEmail()
                    )
                ));
            } else {
                return ResponseEntity.status(401).body(Map.of(
                    "verified", false,
                    "message",  String.format(
                        "Face not recognised (%.0f%% match)", confidence * 100)
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Verification error: " + e.getMessage()));
        }
    }
}
package com.faceauth.faceauth.controller;
import com.faceauth.faceauth.entity.FileEntity;
import com.faceauth.faceauth.entity.User;
import com.faceauth.faceauth.repository.UserRepository;
import com.faceauth.faceauth.service.EmailService;
import com.faceauth.faceauth.service.FileService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EmailService emailService;

    @PostMapping("/save")
    public ResponseEntity<FileEntity> saveFile(@RequestBody FileEntity fileEntity) {
        FileEntity savedFile = fileService.saveFile(fileEntity);
        return ResponseEntity.ok(savedFile);
    }
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        try {
            String originalName = file.getOriginalFilename();
            String storedName = UUID.randomUUID().toString() + "_" + originalName;
            String uploadDir = "C:/FaceAuthWebApp/uploads/user_" + userId;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Files.copy(
                    file.getInputStream(),
                    uploadPath.resolve(storedName),
                    StandardCopyOption.REPLACE_EXISTING
            );
            FileEntity entity = new FileEntity();
            entity.setUserId(userId);
            entity.setFileName(originalName);
            entity.setStoredFileName(storedName);
            entity.setFilePath(uploadPath.resolve(storedName).toString());
            entity.setFileSize(file.getSize());
            entity.setUploadTime(java.time.LocalDateTime.now());
            fileService.saveFile(entity);

            // ── Send upload notification email (does not block upload if it fails) ──
            try {
                User user = userRepo.findById(userId).orElse(null);
                if (user != null) {
                    emailService.sendUploadAlert(user.getEmail(), user.getName(), originalName);
                }
            } catch (Exception mailEx) {
                System.out.println("[FileController] Failed to send upload email: " + mailEx.getMessage());
            }

            return ResponseEntity.ok("File uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FileEntity>> getUserFiles(@PathVariable Long userId) {
        List<FileEntity> files = fileService.getFilesByUserId(userId);
        return ResponseEntity.ok(files);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return ResponseEntity.ok("File deleted successfully");
    }
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) throws Exception {
        Path path = Paths.get("C:/FaceAuthWebApp/uploads/" + filename);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
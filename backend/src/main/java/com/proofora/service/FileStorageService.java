package com.proofora.service;

import com.proofora.config.ProoforaProperties;
import com.proofora.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final Path root;

    public FileStorageService(ProoforaProperties properties) throws IOException {
        this.root = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String storeResume(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Resume file is empty");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED.contains(contentType) && !allowedName(file.getOriginalFilename())) {
            throw ApiException.badRequest("Only PDF, DOC, DOCX, or TXT resumes are allowed");
        }
        String ext = extension(file.getOriginalFilename());
        String key = "resumes/" + userId + "/" + UUID.randomUUID() + ext;
        Path dest = root.resolve(key).normalize();
        if (!dest.startsWith(root)) {
            throw ApiException.badRequest("Invalid storage path");
        }
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Could not store resume", e);
        }
    }

    public String storeImage(Long userId, String folder, MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("The image is empty.");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw ApiException.badRequest("Images must be 2 MB or smaller.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext;
        if (name.endsWith(".png")) {
            ext = ".png";
        } else if (name.endsWith(".webp")) {
            ext = ".webp";
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            ext = ".jpg";
        } else {
            throw ApiException.badRequest("Use a JPG, PNG, or WEBP image.");
        }
        String key = folder + "/" + userId + "/" + UUID.randomUUID() + ext;
        Path dest = root.resolve(key).normalize();
        if (!dest.startsWith(root)) {
            throw ApiException.badRequest("Invalid storage path");
        }
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Could not store image", e);
        }
    }

    public Path resolve(String key) {
        Path dest = root.resolve(key).normalize();
        if (!dest.startsWith(root)) {
            throw ApiException.forbidden("Invalid file key");
        }
        return dest;
    }

    private boolean allowedName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".doc") || lower.endsWith(".docx");
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            return ".bin";
        }
        return name.substring(name.lastIndexOf('.'));
    }
}

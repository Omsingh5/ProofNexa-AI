package com.proofora.web;

import com.proofora.dto.Dtos;
import com.proofora.security.SecurityUtil;
import com.proofora.service.ProfileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public Object me() {
        return profiles.me(SecurityUtil.currentUser());
    }

    @PutMapping
    public Object update(@RequestBody Dtos.ProfileUpdateRequest request) {
        return profiles.update(SecurityUtil.currentUser(), request);
    }

    @PostMapping("/resume")
    public void upload(@RequestParam("file") MultipartFile file) {
        profiles.uploadResume(SecurityUtil.currentUser(), file);
    }

    @PostMapping("/photo")
    public void photo(@RequestParam("file") MultipartFile file) {
        profiles.uploadPhoto(SecurityUtil.currentUser(), file);
    }

    @GetMapping("/photo")
    public ResponseEntity<Resource> photo() {
        return profiles.ownImage(SecurityUtil.currentUser(), false);
    }

    @PostMapping("/logo")
    public void logo(@RequestParam("file") MultipartFile file) {
        profiles.uploadLogo(SecurityUtil.currentUser(), file);
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> logo() {
        return profiles.ownImage(SecurityUtil.currentUser(), true);
    }

    @GetMapping("/{userId}/resume")
    public ResponseEntity<Resource> download(@PathVariable Long userId) {
        return profiles.downloadResume(SecurityUtil.currentUser(), userId);
    }
}

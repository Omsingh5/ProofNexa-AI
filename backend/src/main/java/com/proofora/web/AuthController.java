package com.proofora.web;

import com.proofora.dto.Dtos;
import com.proofora.security.SecurityUtil;
import com.proofora.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public Dtos.AuthResponse register(@Valid @RequestBody Dtos.RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public Dtos.AuthResponse login(@Valid @RequestBody Dtos.LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        auth.logout(SecurityUtil.currentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public Dtos.ForgotPasswordResponse forgot(@Valid @RequestBody Dtos.ForgotPasswordRequest request,
                                              HttpServletRequest http) {
        return auth.forgotPassword(request.email(), clientKey(http, request.email()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody Dtos.ResetPasswordRequest request,
                                      HttpServletRequest http) {
        auth.resetPassword(request.token(), request.password(), clientKey(http, "reset"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivate(@Valid @RequestBody Dtos.DeactivateRequest request) {
        auth.deactivate(SecurityUtil.currentUser(), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Dtos.UserView me() {
        return Dtos.UserView.from(SecurityUtil.currentUser());
    }

    private static String clientKey(HttpServletRequest request, String suffix) {
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        return ip + ":" + suffix;
    }
}

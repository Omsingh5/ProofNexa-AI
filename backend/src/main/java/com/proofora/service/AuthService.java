package com.proofora.service;

import com.proofora.config.ProoforaProperties;
import com.proofora.domain.CandidateProfile;
import com.proofora.domain.PasswordResetToken;
import com.proofora.domain.RecruiterProfile;
import com.proofora.domain.Role;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.CandidateProfileRepository;
import com.proofora.repo.PasswordResetTokenRepository;
import com.proofora.repo.RecruiterProfileRepository;
import com.proofora.repo.UserRepository;
import com.proofora.security.JwtService;
import com.proofora.security.LoginAttemptLimiter;
import com.proofora.security.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String LOGIN_BUCKET = "login";
    private static final String RESET_BUCKET = "reset";
    private static final String FORGOT_MESSAGE =
            "If an account exists for that email, password reset instructions have been created.";

    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final RecruiterProfileRepository recruiters;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuditService audit;
    private final LoginAttemptLimiter attempts;
    private final ProoforaProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, CandidateProfileRepository candidates,
                       RecruiterProfileRepository recruiters, PasswordResetTokenRepository resetTokens,
                       PasswordEncoder encoder, JwtService jwtService, AuditService audit,
                       LoginAttemptLimiter attempts, ProoforaProperties properties) {
        this.users = users;
        this.candidates = candidates;
        this.recruiters = recruiters;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.audit = audit;
        this.attempts = attempts;
        this.properties = properties;
    }

    @Transactional
    public Dtos.AuthResponse register(Dtos.RegisterRequest request) {
        PasswordPolicy.requireStrong(request.password());
        if (users.existsByEmail(request.email().toLowerCase(Locale.ROOT))) {
            throw ApiException.conflict("Email already registered");
        }
        Role role = request.role() == null ? Role.CANDIDATE : request.role();
        if (role == Role.ADMIN) {
            throw ApiException.forbidden("Admin accounts cannot be self-registered");
        }
        User user = new User();
        user.setEmail(request.email().toLowerCase(Locale.ROOT));
        user.setPasswordHash(encoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(role);
        user.setEnabled(true);
        users.save(user);
        if (role == Role.CANDIDATE) {
            CandidateProfile profile = new CandidateProfile();
            profile.setUser(user);
            candidates.save(profile);
        } else {
            RecruiterProfile profile = new RecruiterProfile();
            profile.setUser(user);
            profile.setCompanyName(request.companyName());
            recruiters.save(profile);
        }
        audit.record(user, "REGISTER", "User", user.getId(), role.name());
        return new Dtos.AuthResponse(jwtService.issue(user), Dtos.UserView.from(user));
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        ProoforaProperties.Auth auth = properties.getAuth();
        attempts.assertAllowed(LOGIN_BUCKET, email, auth.getLoginMaxAttempts(), auth.getLoginWindowSeconds());
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            attempts.recordFailure(LOGIN_BUCKET, email, auth.getLoginWindowSeconds());
            throw ApiException.unauthorized("Invalid email or password");
        }
        attempts.clear(LOGIN_BUCKET, email);
        if (!user.isEnabled()) {
            throw ApiException.forbidden("This account is deactivated");
        }
        audit.record(user, "LOGIN", "User", user.getId(), null);
        return new Dtos.AuthResponse(jwtService.issue(user), Dtos.UserView.from(user));
    }

    public void logout(User user) {
        audit.record(user, "LOGOUT", "User", user.getId(), "client session cleared; token remains valid until expiry");
    }

    @Transactional
    public Dtos.ForgotPasswordResponse forgotPassword(String email, String rateKey) {
        String normalized = email.toLowerCase(Locale.ROOT);
        ProoforaProperties.Auth auth = properties.getAuth();
        attempts.assertAllowed(RESET_BUCKET, rateKey, auth.getResetMaxAttempts(), auth.getLoginWindowSeconds());
        attempts.recordFailure(RESET_BUCKET, rateKey, auth.getLoginWindowSeconds());
        User user = users.findByEmail(normalized).orElse(null);
        String exposed = null;
        if (user != null && user.isEnabled()) {
            resetTokens.deleteAll(resetTokens.findByUserAndUsedAtIsNull(user));
            String raw = newToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(sha256(raw));
            token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(auth.getResetTokenMinutes())));
            resetTokens.save(token);
            audit.record(user, "PASSWORD_RESET_REQUEST", "User", user.getId(), "email delivery not configured");
            log.info("Password reset stored for userId={}. Email delivery is not configured.", user.getId());
            if (auth.isExposeResetToken()) {
                exposed = raw;
            }
        }
        return new Dtos.ForgotPasswordResponse(FORGOT_MESSAGE, exposed);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String rateKey) {
        PasswordPolicy.requireStrong(newPassword);
        ProoforaProperties.Auth auth = properties.getAuth();
        attempts.assertAllowed(RESET_BUCKET, rateKey, auth.getResetMaxAttempts(), auth.getLoginWindowSeconds());
        PasswordResetToken token = resetTokens.findByTokenHash(sha256(rawToken)).orElse(null);
        if (token == null || token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            attempts.recordFailure(RESET_BUCKET, rateKey, auth.getLoginWindowSeconds());
            throw ApiException.badRequest("Reset link is invalid or expired");
        }
        User user = token.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        token.setUsedAt(Instant.now());
        attempts.clear(LOGIN_BUCKET, user.getEmail());
        audit.record(user, "PASSWORD_RESET", "User", user.getId(), null);
    }

    @Transactional
    public void deactivate(User user, String password) {
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        user.setEnabled(false);
        users.save(user);
        audit.record(user, "DEACTIVATE", "User", user.getId(), null);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

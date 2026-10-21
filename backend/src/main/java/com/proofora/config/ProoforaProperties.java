package com.proofora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proofora")
public class ProoforaProperties {
    private String uploadDir = "uploads";
    private final Jwt jwt = new Jwt();
    private final Ai ai = new Ai();
    private final Gemini gemini = new Gemini();
    private final Github github = new Github();
    private final Auth auth = new Auth();
    private final Cors cors = new Cors();

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public Jwt getJwt() { return jwt; }
    public Ai getAi() { return ai; }
    public Gemini getGemini() { return gemini; }
    public Github getGithub() { return github; }
    public Auth getAuth() { return auth; }
    public Cors getCors() { return cors; }

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class Ai {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private int dailyLimitPerUser = 40;
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDailyLimitPerUser() { return dailyLimitPerUser; }
        public void setDailyLimitPerUser(int dailyLimitPerUser) { this.dailyLimitPerUser = dailyLimitPerUser; }
    }

    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-3.8-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Github {
        private String token = "";
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    public static class Auth {
        private int loginMaxAttempts = 8;
        private long loginWindowSeconds = 900;
        private int resetMaxAttempts = 5;
        private boolean exposeResetToken = false;
        private int resetTokenMinutes = 30;

        public int getLoginMaxAttempts() { return loginMaxAttempts; }
        public void setLoginMaxAttempts(int loginMaxAttempts) { this.loginMaxAttempts = loginMaxAttempts; }
        public long getLoginWindowSeconds() { return loginWindowSeconds; }
        public void setLoginWindowSeconds(long loginWindowSeconds) { this.loginWindowSeconds = loginWindowSeconds; }
        public int getResetMaxAttempts() { return resetMaxAttempts; }
        public void setResetMaxAttempts(int resetMaxAttempts) { this.resetMaxAttempts = resetMaxAttempts; }
        public boolean isExposeResetToken() { return exposeResetToken; }
        public void setExposeResetToken(boolean exposeResetToken) { this.exposeResetToken = exposeResetToken; }
        public int getResetTokenMinutes() { return resetTokenMinutes; }
        public void setResetTokenMinutes(int resetTokenMinutes) { this.resetTokenMinutes = resetTokenMinutes; }
    }

    public static class Cors {
        private String allowedOriginPatterns = "http://localhost:*,http://127.0.0.1:*";

        public String getAllowedOriginPatterns() { return allowedOriginPatterns; }
        public void setAllowedOriginPatterns(String allowedOriginPatterns) { this.allowedOriginPatterns = allowedOriginPatterns; }

        public java.util.List<String> patterns() {
            return java.util.Arrays.stream(allowedOriginPatterns.split(","))
                    .map(String::trim)
                    .filter(pattern -> !pattern.isEmpty())
                    .toList();
        }
    }
}

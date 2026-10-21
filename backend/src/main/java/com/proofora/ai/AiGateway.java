package com.proofora.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofora.config.ProoforaProperties;
import com.proofora.domain.AiUsage;
import com.proofora.domain.User;
import com.proofora.exception.ApiException;
import com.proofora.repo.AiUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiGateway {
    private static final Logger log = LoggerFactory.getLogger(AiGateway.class);

    private final ProoforaProperties properties;
    private final AiUsageRepository usage;
    private final ObjectMapper mapper;
    private final RestClient http = RestClient.create();

    public AiGateway(ProoforaProperties properties, AiUsageRepository usage, ObjectMapper mapper) {
        this.properties = properties;
        this.usage = usage;
        this.mapper = mapper;
    }

    public boolean isLive() {
        return properties.getAi().getApiKey() != null && !properties.getAi().getApiKey().isBlank();
    }

    public String complete(User user, String system, String userMessage) {
        if (!isLive()) {
            return null;
        }
        enforceLimit(user);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", properties.getAi().getModel());
            body.put("temperature", 0.2);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", userMessage)
            ));
            String json = http.post()
                    .uri(trimSlash(properties.getAi().getBaseUrl()) + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getAi().getApiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = mapper.readTree(json);
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.warn("AI provider call failed, using heuristic fallback: {}", e.getMessage());
            return null;
        }
    }

    public void consumeQuota(User user) {
        enforceLimit(user);
    }

    private void enforceLimit(User user) {
        if (user == null) {
            return;
        }
        if (!user.isAiConsent()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AI analysis requires consent. Update it in your profile.");
        }
        LocalDate today = LocalDate.now();
        AiUsage row = usage.findByUserAndUsageDate(user, today).orElseGet(() -> {
            AiUsage created = new AiUsage();
            created.setUser(user);
            created.setUsageDate(today);
            created.setCallCount(0);
            return created;
        });
        if (row.getCallCount() >= properties.getAi().getDailyLimitPerUser()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Daily AI limit reached. Try again tomorrow.");
        }
        row.setCallCount(row.getCallCount() + 1);
        usage.save(row);
    }

    private String trimSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

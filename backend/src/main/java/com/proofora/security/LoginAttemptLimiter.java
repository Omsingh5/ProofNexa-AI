package com.proofora.security;

import com.proofora.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginAttemptLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void assertAllowed(String bucket, String key, int maxAttempts, long windowSeconds) {
        Window window = current(bucket, key, windowSeconds);
        if (window.count.get() >= maxAttempts) {
            throw ApiException.tooManyRequests("Too many attempts. Try again later.");
        }
    }

    public void recordFailure(String bucket, String key, long windowSeconds) {
        current(bucket, key, windowSeconds).count.incrementAndGet();
    }

    public void clear(String bucket, String key) {
        windows.remove(id(bucket, key));
    }

    private Window current(String bucket, String key, long windowSeconds) {
        long now = System.currentTimeMillis() / 1000;
        return windows.compute(id(bucket, key), (ignored, existing) -> {
            if (existing == null || now - existing.start >= windowSeconds) {
                return new Window(now);
            }
            return existing;
        });
    }

    private static String id(String bucket, String key) {
        return bucket + ":" + (key == null ? "" : key.toLowerCase());
    }

    private static final class Window {
        private final long start;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long start) {
            this.start = start;
        }
    }
}

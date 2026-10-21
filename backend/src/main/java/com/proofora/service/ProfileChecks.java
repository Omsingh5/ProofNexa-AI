package com.proofora.service;

import com.proofora.exception.ApiException;

import java.time.Year;
import java.util.List;

public final class ProfileChecks {
    private ProfileChecks() {}

    public static String text(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw ApiException.badRequest("That field is too long.");
        }
        return trimmed;
    }

    public static String optionalUrl(String value, String label) {
        String trimmed = text(value, 300);
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            throw ApiException.badRequest(label + " must start with http:// or https://.");
        }
        return trimmed;
    }

    public static String phone(String value) {
        String trimmed = text(value, 30);
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!trimmed.matches("[0-9+() .\\-]{6,30}")) {
            throw ApiException.badRequest("Enter a phone number using digits and optional + ( ) -.");
        }
        return trimmed;
    }

    public static Integer year(Integer value) {
        if (value == null) {
            return null;
        }
        int max = Year.now().getValue() + 6;
        if (value < 1950 || value > max) {
            throw ApiException.badRequest("Graduation year must be between 1950 and " + max + ".");
        }
        return value;
    }

    public static Integer years(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 60) {
            throw ApiException.badRequest("Years of experience must be between 0 and 60.");
        }
        return value;
    }

    public static <T> List<T> limit(List<T> values) {
        if (values == null) {
            return List.of();
        }
        if (values.size() > 15) {
            throw ApiException.badRequest("Add at most 15 entries in each section.");
        }
        return values;
    }

    public static int completion(int filled, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(filled * 100.0 / total);
    }

    public static int filled(boolean... flags) {
        int count = 0;
        for (boolean flag : flags) {
            if (flag) {
                count++;
            }
        }
        return count;
    }
}

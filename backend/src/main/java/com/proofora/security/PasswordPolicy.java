package com.proofora.security;

import com.proofora.exception.ApiException;

public final class PasswordPolicy {
    public static final String REQUIREMENTS =
            "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, and a digit";

    private PasswordPolicy() {}

    public static void requireStrong(String password) {
        if (!isStrong(password)) {
            throw ApiException.badRequest(REQUIREMENTS);
        }
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
        }
        return upper && lower && digit;
    }
}

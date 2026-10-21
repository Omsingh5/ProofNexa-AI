package com.proofora.ai;

import com.proofora.domain.Role;

import java.util.Locale;

public final class NexaIntents {
    private NexaIntents() {}

    public static String feature(Role role, String message) {
        return feature(role, message, null);
    }

    public static String feature(Role role, String message, String previousFeature) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        boolean recruiter = role == Role.RECRUITER || role == Role.ADMIN;
        if (isGreeting(q)) {
            return "greet";
        }
        if (isFollowUp(q)) {
            if (previousFeature != null && !previousFeature.equals("greet") && !previousFeature.equals("help")) {
                return previousFeature;
            }
            return recruiter ? "jd" : "jobs";
        }
        if (isRoleTitle(q)) {
            return recruiter ? "jd" : "jobs";
        }
        if (q.contains("reject") || q.contains("accept applicant") || q.contains("hire this") || q.contains("decline applicant")) {
            return "refuse-decision";
        }
        if (!recruiter && (q.contains("apply to") || q.contains("submit my application") || q.contains("submit application"))) {
            return "refuse-apply";
        }
        if (isHelpRequest(q) && !asksForData(q)) {
            return "capabilities";
        }
        if (contains(q, "resume", "cv")) {
            return recruiter ? "own-resume" : "resume";
        }
        boolean aboutJob = contains(q, "job", "role", "posting", "position", "opening");
        boolean aboutFit = contains(q, "fit", "fits", "best", "suit", "recommend", "which", "match", "for me", "more me");
        if (recruiter) {
            if (aboutJob && aboutFit) return "focus";
            if (contains(q, "analytics", "statistic", "how many", "numbers", "trend")) return "analytics";
            if (contains(q, "description", "rewrite", "draft", "write a", "jd")) return "jd";
            if (contains(q, "interview", "question", "assessment")) return "interview-plan";
            if (contains(q, "evidence", "github", "quality", "technical")) return "evidence";
            if (contains(q, "applicant", "candidate", "who applied", "qualification")) return "applicants";
            if (contains(q, "application", "pipeline", "status", "reviewing", "offer")) return "pipeline";
            return "help";
        }
        if (contains(q, "challenge", "assignment", "debug")) return "challenge";
        if (contains(q, "roadmap", "learning plan", "milestone")) return "roadmap";
        if (contains(q, "answer:")) return "interview";
        if (contains(q, "interview")) return "interview";
        if (contains(q, "application", "applied", "status")) return "applications";
        if (contains(q, "github", "repository", "project", "architecture", "quality")) return "github";
        if (contains(q, "gap", "missing", "unconfirmed", "compare")) return "compare";
        if (contains(q, "skill")) return "resume";
        if (aboutJob || aboutFit) return "jobs";
        return "help";
    }

    private static boolean isHelpRequest(String q) {
        return contains(q, "how can you", "what can you", "who are you", "what do you do", "help me", "your help", "can you help", "help with");
    }

    private static boolean asksForData(String q) {
        return contains(q, "pipeline", "applicant", "candidate", "status", "analytics", "draft", "description",
                "interview", "summarize", "summary", "show", "list", "which", "best", "fit", "github", "resume", "cv",
                "challenge", "roadmap");
    }

    private static boolean isGreeting(String q) {
        return q.matches("(?i)(hi|hii|hello|hey|thanks|thank you|good morning|good evening|good afternoon)[!. ]*");
    }

    private static boolean isFollowUp(String q) {
        return q.matches("(?i)(do it|do that|yes|yeah|yep|ok|okay|sure|go ahead|write it|please|please do|continue|that one)[!. ]*");
    }

    private static boolean isRoleTitle(String q) {
        if (q.length() > 60 || q.contains("?")) {
            return false;
        }
        int words = q.split("\\s+").length;
        return words >= 1 && words <= 6 && q.matches(".*\\b(engineer|developer|intern|designer|analyst|manager|architect)\\b.*");
    }

    private static boolean contains(String q, String... needles) {
        for (String needle : needles) {
            if (q.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

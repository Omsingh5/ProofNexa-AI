package com.proofora.ai;

import java.util.Locale;

/**
 * Short, general career guidance. Account numbers are added separately and are never invented here.
 */
public final class NexaKnowledge {
    private NexaKnowledge() {}

    public static String relevant(String message) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        if (contains(q, "ats", "applicant tracking", "keyword scan")) {
            sb.append("An ATS is software that stores applications and sometimes scans a resume for required words. ProofNexa does not produce an ATS percentage. Match a resume to a job by putting the job's real skills in a skills section and tying each one to a project or result.\n");
        }
        if (contains(q, "resume", "cv ", "curriculum")) {
            sb.append("A useful resume leads with the role you want, then proof: what you built, the tools, and the result. Keep the skills list limited to tools you can discuss.\n");
        }
        if (contains(q, "interview")) {
            sb.append("In a technical interview, answer with one situation, the decision you made, and the file or test that shows it. Guessing a metric you did not measure is worse than saying what you have not measured.\n");
        }
        if (contains(q, "github", "repository", "pull request")) {
            sb.append("A repository demonstrates a skill only when the skill appears in the code or tests, not only in the README. Reviewers look for a way to run it, a test, and no secrets in the files.\n");
        }
        if (contains(q, "skill")) {
            sb.append("Treat a skill as confirmed only when a resume line, a project file, or a completed challenge shows it. A word in a headline is a claim, not evidence.\n");
        }
        if (contains(q, "career", "promotion", "switch career", "roadmap")) {
            sb.append("A practical career step is one missing skill, one small public example, and one way you will explain it in an interview. Broad plans fail when they are not tied to a target role.\n");
        }
        if (contains(q, "recruit", "hiring", "job description")) {
            sb.append("A clear job description names the work, the evidence you will accept, and the skills that are required rather than a long wishlist. Status changes such as interview or rejection stay with the recruiter.\n");
        }
        return sb.toString().trim();
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

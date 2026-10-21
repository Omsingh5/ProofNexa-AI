package com.proofora.service;

import com.proofora.ai.ChatUpload;
import com.proofora.ai.GeminiClient;
import com.proofora.ai.NexaIntents;
import com.proofora.ai.NexaKnowledge;
import com.proofora.domain.Role;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class NexaService {
    private final AssistantService assistant;
    private final ResumeAiService resumeAi;
    private final GithubAnalysisService github;
    private final QualityAnalyzerService quality;
    private final CareerGrowthService growth;
    private final ApplicationService applications;
    private final JobService jobs;
    private final AnalyticsService analytics;
    private final GeminiClient gemini;

    public NexaService(AssistantService assistant, ResumeAiService resumeAi, GithubAnalysisService github,
                       QualityAnalyzerService quality, CareerGrowthService growth, ApplicationService applications,
                       JobService jobs, AnalyticsService analytics, GeminiClient gemini) {
        this.assistant = assistant;
        this.resumeAi = resumeAi;
        this.github = github;
        this.quality = quality;
        this.growth = growth;
        this.applications = applications;
        this.jobs = jobs;
        this.analytics = analytics;
        this.gemini = gemini;
    }

    @Transactional
    public Dtos.NexaReply ask(User user, String message, String confirmAction) {
        return ask(user, message, confirmAction, java.util.List.of());
    }

    @Transactional
    public Dtos.NexaReply ask(User user, String message, String confirmAction, java.util.List<ChatUpload> uploads) {
        if ((message == null || message.isBlank()) && (uploads == null || uploads.isEmpty())) {
            message = confirmAction == null ? "help" : "Confirmed.";
        }
        if (message == null || message.isBlank()) {
            message = "Please review the attached file.";
        }
        List<Dtos.AssistantMessageView> prior = assistant.history(user);
        String previousFeature = previousFeature(prior);
        String stored = message;
        if (uploads != null && !uploads.isEmpty()) {
            stored = message + "\n" + uploads.stream()
                    .map(file -> "[Attached: " + file.filename() + ", " + file.data().length + " bytes]")
                    .reduce((a, b) -> a + "\n" + b).orElse("");
        }
        assistant.remember(user, "user", trim(stored), "nexa");
        Dtos.NexaReply reply = confirmAction != null && !confirmAction.isBlank()
                ? confirm(user, confirmAction)
                : answer(user, message, previousFeature, prior, uploads == null ? java.util.List.of() : uploads);
        assistant.remember(user, "assistant", trim(reply.content()), "nexa|" + reply.feature()
                + (reply.pendingAction() == null ? "" : "|pending|" + reply.pendingAction()));
        return reply;
    }

    private Dtos.NexaReply answer(User user, String message, String previousFeature,
                                  List<Dtos.AssistantMessageView> prior, List<ChatUpload> uploads) {
        String feature = NexaIntents.feature(user.getRole(), message, previousFeature);
        String content = switch (feature) {
            case "greet" -> greet(user);
            case "capabilities" -> capabilities(user);
            case "refuse-decision" -> "I can't accept, reject, or hire applicants. Update the status yourself on the hiring desk.";
            case "refuse-apply" -> "I won't submit an application for you. Open the job and choose Apply when you are ready.";
            case "resume" -> resume(user) + jobAddon(user, message);
            case "own-resume" -> ownResume(user);
            case "jobs" -> jobs(user, message);
            case "compare", "gap" -> compare(user, message);
            case "github" -> github(user);
            case "roadmap" -> roadmap(user, message);
            case "interview" -> interview(user, message);
            case "applications" -> applications(user);
            case "challenge" -> challenge(user, message);
            case "focus" -> focus(user);
            case "jd" -> jd(user, message);
            case "applicants" -> applicants(user, message);
            case "evidence" -> evidence(user);
            case "interview-plan" -> interviewPlan(user, message);
            case "pipeline" -> pipeline(user, message);
            case "analytics" -> analytics(user);
            default -> converse(user, message);
        };
        String pending = pendingFrom(content);
        if (pending != null) {
            content = content.substring(0, content.indexOf("\nPENDING:")).trim();
        }
        content = speak(user, message, content, prior, uploads);
        return new Dtos.NexaReply(content, feature, pending, Instant.now());
    }

    private Dtos.NexaReply confirm(User user, String action) {
        if (user.getRole() != Role.CANDIDATE) {
            return reply("confirm", "Only a candidate account can run that saved action.");
        }
        if ("ANALYZE_RESUME".equals(action)) {
            Dtos.SkillExtractionView extracted = resumeAi.analyzeResume(user);
            List<String> skills = extracted.userCorrected() && extracted.correctedSkills() != null
                    ? extracted.correctedSkills() : extracted.extractedSkills();
            return reply("resume", "I read the resume on file. Skills: " + join(skills) + "\n" + extracted.summary());
        }
        if (action.startsWith("ROADMAP:")) {
            long jobId = Long.parseLong(action.substring("ROADMAP:".length()));
            Dtos.RoadmapView roadmap = growth.generateRoadmap(user, jobId, null);
            return reply("roadmap", roadmap.title() + "\n\n" + shorten(roadmap.contentMarkdown(), 1800));
        }
        if (action.startsWith("QUALITY:")) {
            long projectId = Long.parseLong(action.substring("QUALITY:".length()));
            Dtos.QualityReportView report = quality.analyze(user, projectId, com.proofora.domain.AnalysisMode.VALIDATION);
            return reply("github", report.evidenceSummary() + "\n" + report.architectureNotes() + "\n" + report.recommendations());
        }
        return reply("help", "That confirmation is not one of the supported actions.");
    }

    private String resume(User user) {
        Dtos.SkillExtractionView latest = resumeAi.latest(user);
        List<Dtos.RecommendationView> recs = resumeAi.recommend(user);
        if (latest == null || latest.extractedSkills() == null || latest.extractedSkills().isEmpty()) {
            return """
                    No resume skills are stored yet. I can read the uploaded resume and fill the skill list.
                    PENDING:ANALYZE_RESUME""";
        }
        List<String> skills = latest.userCorrected() && latest.correctedSkills() != null
                ? latest.correctedSkills() : latest.extractedSkills();
        StringBuilder sb = new StringBuilder("Skills on file: ").append(join(skills)).append(".\n");
        sb.append(latest.summary() == null ? "" : latest.summary()).append("\n");
        if (!recs.isEmpty()) {
            Dtos.RecommendationView top = recs.get(0);
            Dtos.SkillGapView gap = resumeAi.gap(user, top.job().id());
            sb.append("Against ").append(top.job().title()).append(" (").append(gap.matchScore()).append("%): ");
            sb.append(gap.explanation()).append("\n");
            if (!gap.missingSkills().isEmpty()) {
                sb.append("Improve the resume by showing evidence for: ").append(join(gap.missingSkills())).append(".");
            }
        }
        return sb.toString();
    }

    private String jobs(User user, String message) {
        String q = message.toLowerCase(Locale.ROOT);
        boolean locationAsked = mentionsLocation(q);
        boolean titleAsked = q.matches(".*\\b(engineer|developer|intern|designer|analyst|manager|architect)\\b.*");
        List<String> lines = resumeAi.recommend(user).stream()
                .filter(r -> !locationAsked || (r.job().location() != null && q.contains(r.job().location().toLowerCase(Locale.ROOT))))
                .filter(r -> !titleAsked || sharesWord(q, r.job().title()))
                .limit(5)
                .map(r -> r.matchScore() + "% " + r.job().title() + " · " + r.job().location() + " · " + r.reason())
                .toList();
        if (lines.isEmpty()) {
            return "No open roles matched that search in the current job list.";
        }
        String best = lines.get(0);
        String rest = lines.size() == 1 ? "" : "\nOther open roles:\n- " + String.join("\n- ", lines.subList(1, lines.size()));
        return "The closest open role for you is " + best + "." + rest;
    }

    private String focus(User user) {
        requireRecruiter(user);
        List<Dtos.JobView> mine = jobs.mine(user);
        if (mine.isEmpty()) {
            return "You have not published a role yet. Send a title such as software engineer and I will draft one.";
        }
        Dtos.JobView best = null;
        long bestPending = -1;
        long bestTotal = -1;
        StringBuilder sb = new StringBuilder("This account hires, so “best fit” means the posting that needs you next.\n");
        for (Dtos.JobView job : mine) {
            List<Dtos.ApplicationView> apps = applications.forJob(user, job.id());
            long pending = apps.stream().filter(a -> a.status().name().equals("APPLIED") || a.status().name().equals("REVIEWING")).count();
            sb.append("- ").append(job.title()).append(": ").append(apps.size()).append(" applications, ")
                    .append(pending).append(" waiting on you");
            if (!apps.isEmpty()) {
                sb.append(" (");
                sb.append(apps.stream().map(a -> a.candidateName() + " " + a.status()).reduce((a, b) -> a + "; " + b).orElse(""));
                sb.append(")");
            }
            sb.append(".\n");
            if (pending > bestPending || (pending == bestPending && apps.size() > bestTotal)) {
                best = job;
                bestPending = pending;
                bestTotal = apps.size();
            }
        }
        sb.append("Start with ").append(best.title());
        if (bestPending > 0) {
            sb.append(" because ").append(bestPending).append(" application(s) are still before interview.");
        } else {
            sb.append(" because it is the posting with the most candidate activity.");
        }
        sb.append(" I will not change any status.");
        return sb.toString();
    }

    private boolean sharesWord(String q, String title) {
        if (title == null) {
            return false;
        }
        String hay = title.toLowerCase(Locale.ROOT);
        for (String word : q.split("\\s+")) {
            if (word.length() > 3 && hay.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean mentionsLocation(String q) {
        return q.contains("remote") || q.contains("bengaluru") || q.contains("hyderabad") || q.contains(" in ");
    }

    private String compare(User user, String message) {
        List<Dtos.RecommendationView> recs = resumeAi.recommend(user);
        Dtos.RecommendationView chosen = recs.stream()
                .filter(r -> message.toLowerCase(Locale.ROOT).contains(r.job().title().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(recs.isEmpty() ? null : recs.get(0));
        if (chosen == null) {
            return "There are no open jobs to compare.";
        }
        Dtos.SkillGapView gap = resumeAi.gap(user, chosen.job().id());
        List<Dtos.GithubProjectView> projects = github.list(user);
        String proven = projects.stream()
                .flatMap(p -> (p.detectedTechnologies() == null ? List.<String>of() : p.detectedTechnologies()).stream())
                .distinct()
                .toList()
                .toString();
        return chosen.job().title() + " requires " + join(chosen.job().requiredSkills())
                + ".\nMatched on the profile: " + join(gap.matchedSkills())
                + ".\nNot confirmed on the profile: " + join(gap.missingSkills())
                + ".\nTechnologies detected in your imported repositories: " + proven
                + ".\n" + gap.explanation();
    }

    private String github(User user) {
        List<Dtos.GithubProjectView> projects = github.list(user);
        if (projects.isEmpty()) {
            return "No GitHub repository is imported yet. Use Skill proof and import a public repo.";
        }
        StringBuilder sb = new StringBuilder();
        String pending = null;
        for (Dtos.GithubProjectView project : projects.stream().limit(3).toList()) {
            sb.append(project.owner()).append("/").append(project.name()).append(": ")
                    .append(join(project.detectedTechnologies())).append(".\n");
            List<Dtos.QualityReportView> reports = quality.history(user, project.id());
            if (reports.isEmpty()) {
                sb.append("No quality report is saved for this repository yet.\n");
                if (pending == null) {
                    pending = "QUALITY:" + project.id();
                }
            } else {
                Dtos.QualityReportView report = reports.get(0);
                sb.append("Latest ").append(report.mode()).append(" scores — architecture ")
                        .append(report.architectureScore()).append(", maintainability ")
                        .append(report.maintainabilityScore()).append(", security ")
                        .append(report.securityScore()).append(", testing ")
                        .append(report.testingScore()).append(", data ")
                        .append(report.dataDesignScore()).append(".\n")
                        .append(report.architectureNotes()).append("\n")
                        .append(shorten(report.recommendations(), 500)).append("\n");
            }
        }
        if (pending != null) {
            sb.append("I can run a fresh quality review after you confirm.\nPENDING:").append(pending);
        }
        return sb.toString();
    }

    private String roadmap(User user, String message) {
        List<Dtos.RoadmapView> existing = growth.listRoadmaps(user);
        StringBuilder sb = new StringBuilder();
        if (existing.isEmpty()) {
            sb.append("You have no saved roadmap.\n");
        } else {
            Dtos.RoadmapView latest = existing.get(0);
            sb.append(latest.title()).append("\n").append(shorten(latest.contentMarkdown(), 900)).append("\n");
        }
        if (message.toLowerCase(Locale.ROOT).contains("create") || message.toLowerCase(Locale.ROOT).contains("generate") || existing.isEmpty()) {
            List<Dtos.RecommendationView> recs = resumeAi.recommend(user);
            if (!recs.isEmpty()) {
                sb.append("I can create a roadmap for ").append(recs.get(0).job().title()).append(" after you confirm.\nPENDING:ROADMAP:")
                        .append(recs.get(0).job().id());
            }
        }
        return sb.toString();
    }

    private String interview(User user, String message) {
        if (message.toLowerCase(Locale.ROOT).contains("answer:")) {
            String answer = message.substring(message.toLowerCase(Locale.ROOT).indexOf("answer:") + 7).trim();
            return "Practice feedback (nothing was submitted): " + (answer.length() < 40
                    ? "Add a concrete file, tradeoff, or test you would point to."
                    : "You gave a specific answer. Tie one sentence to a file in your repository and one sentence to a requirement on the job.")
                    + " I do not score this as an official interview result.";
        }
        List<Dtos.RecommendationView> recs = resumeAi.recommend(user);
        if (recs.isEmpty()) {
            return "No open job is available to prepare for.";
        }
        Dtos.JobView job = recs.get(0).job();
        return "Practice questions for " + job.title() + " (not an official interview):\n"
                + "1. Which file shows " + join(job.requiredSkills()) + "?\n"
                + "2. What would you test first, and what would you leave untested?\n"
                + "3. Where does data live in this product?\n"
                + "Reply starting with Answer: and I will comment. I will not submit an application or a challenge.";
    }

    private String applications(User user) {
        List<Dtos.ApplicationView> mine = applications.mine(user);
        if (mine.isEmpty()) {
            return "You have no applications.";
        }
        StringBuilder sb = new StringBuilder();
        for (Dtos.ApplicationView app : mine) {
            List<Dtos.TimelineEventView> timeline = applications.timeline(user, app.id());
            sb.append(app.jobTitle()).append(" is ").append(app.status());
            if (app.recruiterFeedback() != null && !app.recruiterFeedback().isBlank()) {
                sb.append(". Feedback: ").append(app.recruiterFeedback());
            }
            sb.append(". History: ");
            sb.append(timeline.stream().map(e -> e.status() + (e.note() == null ? "" : " (" + e.note() + ")")).reduce((a, b) -> a + "; " + b).orElse("none"));
            sb.append(".\nNext: ");
            sb.append(switch (app.status().name()) {
                case "APPLIED", "REVIEWING" -> "wait for the recruiter, or withdraw from Applications if you want to stop.";
                case "INTERVIEW" -> "prepare evidence for the skills on that job. There is no interview time stored in ProofNexa.";
                case "OFFER" -> "read the recruiter feedback and respond outside this chat. I cannot accept an offer.";
                default -> "no status change will be made from this chat.";
            });
            sb.append("\n");
        }
        return sb.toString();
    }

    private String challenge(User user, String message) {
        List<Dtos.ChallengeView> rows = growth.listChallenges(user);
        if (rows.isEmpty()) {
            return "You have no technical challenge. I will not create or complete one unless you use Growth yourself.";
        }
        Dtos.ChallengeView open = rows.stream().filter(c -> c.status().name().equals("OPEN")).findFirst().orElse(rows.get(0));
        StringBuilder sb = new StringBuilder("Challenge: ").append(open.title()).append(" (").append(open.status()).append(")\n")
                .append(open.prompt()).append("\n");
        sb.append("I will not write the submission. ");
        if (message.toLowerCase(Locale.ROOT).contains("debug") || message.length() > 240) {
            sb.append("Check that the text names the skill, includes a test idea, and contains no passwords or keys. ");
        }
        sb.append("Submit it yourself on Growth when it is your own work.");
        if (open.feedback() != null) {
            sb.append("\nStored feedback: ").append(open.feedback());
        }
        return sb.toString();
    }

    private String greet(User user) {
        String name = user.getFullName() == null || user.getFullName().isBlank()
                ? "there" : user.getFullName().split(" ")[0];
        if (user.getRole() == Role.RECRUITER || user.getRole() == Role.ADMIN) {
            List<Dtos.JobView> mine = jobs.mine(user);
            String titles = mine.stream().map(Dtos.JobView::title).limit(4).reduce((a, b) -> a + ", " + b).orElse("none yet");
            return "Hi " + name + ". You have " + mine.size() + " roles on the hiring desk: " + titles
                    + ".\nSend a role name, such as software engineer, and I will draft that job description. "
                    + "You can also ask me to summarize applicants or plan an interview. I will not accept or reject anyone.";
        }
        return "Hi " + name + ". I can use your saved skills, applications, and imported repositories. "
                + "Ask me to find matching jobs, explain your application status, or review a project.";
    }

    private String jd(User user, String message) {
        requireRecruiter(user);
        String title = roleTitle(message);
        if (title == null) {
            title = jobs.mine(user).stream().map(Dtos.JobView::title).findFirst().orElse("Software Engineer");
        }
        String notes = roleTitle(message) == null
                ? "Practical " + title + " role. Responsibilities, required evidence, and how success is reviewed."
                : "Practical " + title + " role based on the recruiter's request: " + message;
        return "Draft for " + title + ":\n" + assistant.writeJd(user, title, notes);
    }

    private String roleTitle(String message) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        if (!q.matches(".*\\b(engineer|developer|intern|designer|analyst|manager|architect)\\b.*") || q.length() > 60) {
            return null;
        }
        String[] words = q.split("\\s+");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }

    private String previousFeature(List<Dtos.AssistantMessageView> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Dtos.AssistantMessageView message = history.get(i);
            if (!"assistant".equals(message.role()) || message.sources() == null || !message.sources().startsWith("nexa|")) {
                continue;
            }
            String[] parts = message.sources().split("\\|");
            return parts.length > 1 ? parts[1] : null;
        }
        return null;
    }

    private String applicants(User user, String message) {
        requireRecruiter(user);
        StringBuilder sb = new StringBuilder();
        for (Dtos.JobView job : jobs.mine(user)) {
            if (!message.toLowerCase(Locale.ROOT).contains(job.title().toLowerCase(Locale.ROOT))
                    && message.toLowerCase(Locale.ROOT).contains(" for ") && !sb.isEmpty()) {
                continue;
            }
            List<Dtos.ApplicationView> apps = applications.forJob(user, job.id());
            sb.append(job.title()).append(" (").append(apps.size()).append(" applications)\n");
            for (Dtos.ApplicationView app : apps) {
                sb.append("- ").append(app.candidateName()).append(" · ").append(app.status());
                if (app.coverLetter() != null && !app.coverLetter().isBlank()) {
                    sb.append(" · cover letter: ").append(shorten(app.coverLetter(), 180));
                }
                sb.append("\n");
            }
        }
        sb.append("Skill evidence and resumes stay on the candidate account. I only summarized applications you own. I will not accept or reject anyone.");
        return sb.toString();
    }

    private String evidence(User user) {
        requireRecruiter(user);
        return applicants(user, "applicants") + "\nGitHub files and quality scores are not shared with recruiters in this version, so I will not invent a technical review.";
    }

    private String interviewPlan(User user, String message) {
        requireRecruiter(user);
        Dtos.JobView job = jobs.mine(user).stream()
                .filter(j -> message.toLowerCase(Locale.ROOT).contains(j.title().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseGet(() -> jobs.mine(user).stream().findFirst().orElseThrow(() -> ApiException.badRequest("Post a job first.")));
        Dtos.InterviewPackView pack = assistant.interview(user, job.id(), null);
        return pack.questionsMarkdown() + "\nThese questions are a plan only. They do not change any application.";
    }

    private String pipeline(User user, String message) {
        requireRecruiter(user);
        String q = message.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (Dtos.JobView job : jobs.mine(user)) {
            List<Dtos.ApplicationView> apps = applications.forJob(user, job.id()).stream()
                    .filter(a -> q.contains("review") ? a.status().name().equals("REVIEWING")
                            : q.contains("offer") ? a.status().name().equals("OFFER")
                            : q.contains("interview") && !q.contains("question") ? a.status().name().equals("INTERVIEW")
                            : q.contains("applied") ? a.status().name().equals("APPLIED")
                            : true)
                    .toList();
            if (apps.isEmpty()) {
                continue;
            }
            sb.append(job.title()).append("\n");
            apps.forEach(a -> sb.append("- ").append(a.candidateName()).append(" · ").append(a.status())
                    .append(" · ").append(a.updatedAt()).append("\n"));
        }
        return sb.isEmpty() ? "No applications matched that filter." : sb + "Change a status on the hiring desk. I will not do it.";
    }

    private String analytics(User user) {
        requireRecruiter(user);
        Dtos.AnalyticsSummary summary = analytics.summarize(user);
        StringBuilder sb = new StringBuilder(summary.narrative()).append("\nYour postings:\n");
        for (Dtos.JobView job : jobs.mine(user)) {
            List<Dtos.ApplicationView> apps = applications.forJob(user, job.id());
            long pending = apps.stream().filter(a -> a.status().name().equals("APPLIED") || a.status().name().equals("REVIEWING")).count();
            sb.append("- ").append(job.title()).append(": ").append(apps.size()).append(" applications, ")
                    .append(pending).append(" still before interview.\n");
        }
        return sb.toString();
    }

    private String ownResume(User user) {
        requireRecruiter(user);
        return "You're signed in as a recruiter, so this account has no candidate resume and I can't suggest jobs for you personally. "
                + "Attach a PDF or Word resume if you want me to read that file, or ask which of your postings needs attention. "
                + "I won't change any application status.";
    }

    private String jobAddon(User user, String message) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (q.contains("job") || q.contains("suggest") || q.contains("recommend")) {
            return "\n\n" + jobs(user, message);
        }
        return "";
    }

    private String capabilities(User user) {
        if (user.getRole() == Role.RECRUITER || user.getRole() == Role.ADMIN) {
            int roles = jobs.mine(user).size();
            return "I can help with the hiring work on your account. You currently have " + roles + " roles. "
                    + "Ask in ordinary language: name a role and I will draft the description, ask which posting needs you and I will rank those roles from the real applications, or ask who applied and I will summarize only those applicants. "
                    + "I can also plan interview questions and explain pipeline counts. "
                    + "I will not accept or reject anyone, and I will not invent a GitHub review that was not shared with you. "
                    + "If you ask a general question, such as what an ATS is, I will label that as general guidance and keep it separate from your account numbers.";
        }
        return "I can help with your career on this account. Ask which open role fits you, what your applications say, what your resume skills show, or what an imported repository actually contains. "
                + "I will use your saved skills, applications, and projects when the question is about you. "
                + "General topics, such as how an ATS reads a resume, are answered as guidance and are not claimed as measurements from your profile. "
                + "I will not apply to a job or write a challenge solution for you.";
    }

    private String converse(User user, String message) {
        String guidance = NexaKnowledge.relevant(message);
        boolean personal = containsPersonal(message);
        String facts = personal ? compactFacts(user) : "";
        StringBuilder sb = new StringBuilder();
        if (!guidance.isBlank()) {
            sb.append("General guidance, not a number from your account:\n").append(guidance);
        }
        if (!facts.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(facts);
        }
        if (sb.isEmpty()) {
            return capabilities(user);
        }
        return sb.toString();
    }

    private String compactFacts(User user) {
        if (user.getRole() == Role.RECRUITER || user.getRole() == Role.ADMIN) {
            return focus(user);
        }
        return jobs(user, "jobs");
    }

    private boolean containsPersonal(String message) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return q.contains("my ") || q.contains(" me") || q.contains("our ") || q.contains("account")
                || q.contains("applicant") || q.contains("application") || q.contains("pipeline");
    }

    private String speak(User user, String question, String facts, List<Dtos.AssistantMessageView> prior, List<ChatUpload> uploads) {
        if (!gemini.isConfigured()) {
            if (uploads != null && !uploads.isEmpty()) {
                return withLocalFiles("GEMINI_API_KEY is not set, so I did not send this file to Gemini. This is the text read on the server:", uploads);
            }
            return facts;
        }
        if (!user.isAiConsent()) {
            return withLocalFiles(facts, uploads)
                    + "\n\nAI consent is off, so this reply stayed on the server and was not sent to Gemini.";
        }
        try {
            String live = gemini.complete(user, systemPrompt(user), prior, question, facts, uploads);
            return live == null || live.isBlank() ? withLocalFiles(facts, uploads) : live;
        } catch (ApiException ex) {
            boolean imageOnly = uploads != null && uploads.stream().anyMatch(ChatUpload::image)
                    && (facts == null || facts.isBlank());
            if (imageOnly) {
                return "I could not read that image just now. Try again in a moment.";
            }
            if (facts != null && !facts.isBlank()) {
                return withLocalFiles(facts, uploads);
            }
            if (uploads != null && !uploads.isEmpty()) {
                String local = localAttachmentText(uploads);
                if (!local.isBlank()) {
                    return local;
                }
            }
            return "I could not reach Gemini just now. Ask again in a moment.";
        }
    }

    private String systemPrompt(User user) {
        String name = user.getFullName() == null || user.getFullName().isBlank() ? "the user" : user.getFullName();
        return """
                You are Nexa, the career and recruitment assistant inside ProofNexa AI.
                The signed-in person is %s and their role is %s.
                Answer the question they actually asked. Do not paste a pipeline, applicant list, or job ranking unless they asked for a list, a status, a ranking, or who is waiting.
                If they ask how you can help, explain the relevant help in a few sentences and ask what they want next.
                A recruiter has no candidate resume. If they ask you to check "my resume" or suggest jobs for themselves, say that, then use an attached file if one is present.
                Treat ACCOUNT FACTS as verified account data. Label anything beyond those facts as general guidance.
                Never invent applicants, scores, interviews, dates, or GitHub reviews.
                Never accept, reject, hire, apply, or write a challenge solution.
                If a file or image is attached, answer from that file.
                Keep the reply under 220 words unless they asked for a draft or a list.
                """.formatted(name, user.getRole());
    }

    private String withLocalFiles(String facts, List<ChatUpload> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return facts;
        }
        StringBuilder sb = new StringBuilder(facts == null ? "" : facts);
        for (ChatUpload upload : uploads) {
            sb.append("\n\n");
            if (upload.image()) {
                sb.append(upload.filename())
                        .append(" is an image. It was not saved.");
            } else {
                sb.append("Text read from ").append(upload.filename()).append(" on the server:\n")
                        .append(shorten(upload.text(), 2000));
            }
        }
        return sb.toString();
    }

    private String localAttachmentText(List<ChatUpload> uploads) {
        StringBuilder sb = new StringBuilder();
        for (ChatUpload upload : uploads) {
            if (upload.image() || upload.text() == null || upload.text().isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("Text read locally from ").append(upload.filename()).append(":\n")
                    .append(shorten(upload.text(), 2000));
        }
        return sb.toString().trim();
    }

    private void requireRecruiter(User user) {
        if (user.getRole() != Role.RECRUITER && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("That assistant action is for recruiters.");
        }
    }

    private String pendingFrom(String content) {
        int i = content.lastIndexOf("PENDING:");
        if (i < 0) {
            return null;
        }
        return content.substring(i + "PENDING:".length()).trim();
    }

    private Dtos.NexaReply reply(String feature, String content) {
        return new Dtos.NexaReply(content, feature, null, Instant.now());
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "none";
        }
        return String.join(", ", values);
    }

    private String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private String trim(String value) {
        return shorten(value, 7500);
    }
}

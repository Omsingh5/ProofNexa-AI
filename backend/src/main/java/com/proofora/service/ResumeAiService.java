package com.proofora.service;

import com.proofora.ai.AiGateway;
import com.proofora.ai.SkillTaxonomy;
import com.proofora.domain.CandidateProfile;
import com.proofora.domain.Job;
import com.proofora.domain.SkillExtraction;
import com.proofora.domain.SkillGapReport;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.CandidateProfileRepository;
import com.proofora.repo.JobRepository;
import com.proofora.repo.SkillExtractionRepository;
import com.proofora.repo.SkillGapReportRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResumeAiService {
    private final CandidateProfileRepository profiles;
    private final FileStorageService storage;
    private final SkillExtractionRepository extractions;
    private final SkillGapReportRepository gaps;
    private final JobRepository jobs;
    private final AiGateway ai;

    public ResumeAiService(CandidateProfileRepository profiles, FileStorageService storage,
                           SkillExtractionRepository extractions, SkillGapReportRepository gaps,
                           JobRepository jobs, AiGateway ai) {
        this.profiles = profiles;
        this.storage = storage;
        this.extractions = extractions;
        this.gaps = gaps;
        this.jobs = jobs;
        this.ai = ai;
    }

    @Transactional
    public Dtos.SkillExtractionView analyzeResume(User candidate) {
        CandidateProfile profile = profiles.findByUser(candidate)
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        String text = resumeText(profile, candidate);
        if (text.isBlank()) {
            throw ApiException.badRequest("Upload a resume or add a profile summary before extracting skills.");
        }
        Set<String> skills = SkillTaxonomy.detect(text);
        skills.addAll(skillsListedInResume(text));
        String summary = skills.isEmpty()
                ? "No technical skills were found in the resume text. Add a Skills section or correct the list below."
                : "Read " + text.length() + " characters from the resume and found " + skills.size() + " skills.";
        String live = ai.complete(candidate,
                "You extract technical skills from resumes. Reply as: SUMMARY: ... then SKILLS: comma-separated list.",
                text.substring(0, Math.min(text.length(), 8000)));
        if (live != null && !live.isBlank()) {
            summary = live;
            skills.addAll(parseSkillsLine(live));
        }
        SkillExtraction extraction = extractions.findFirstByCandidateOrderByUpdatedAtDesc(candidate)
                .orElseGet(SkillExtraction::new);
        extraction.setCandidate(candidate);
        extraction.setResumeText(text.substring(0, Math.min(text.length(), 15000)));
        extraction.setExtractedSkills(new ArrayList<>(skills));
        extraction.setSummary(summary);
        extraction.setUserCorrected(false);
        extraction.setUpdatedAt(java.time.Instant.now());
        extractions.save(extraction);
        return toView(extraction);
    }

    @Transactional
    public Dtos.SkillExtractionView correct(User candidate, List<String> skills) {
        SkillExtraction extraction = extractions.findFirstByCandidateOrderByUpdatedAtDesc(candidate)
                .orElseThrow(() -> ApiException.notFound("No extraction yet"));
        extraction.setCorrectedSkills(skills);
        extraction.setUserCorrected(true);
        extraction.setUpdatedAt(java.time.Instant.now());
        extractions.save(extraction);
        return toView(extraction);
    }

    public Dtos.SkillExtractionView latest(User candidate) {
        return extractions.findFirstByCandidateOrderByUpdatedAtDesc(candidate)
                .map(this::toView)
                .orElse(null);
    }

    @Transactional
    public Dtos.SkillGapView gap(User candidate, Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job not found"));
        List<String> candidateSkills = extractions.findFirstByCandidateOrderByUpdatedAtDesc(candidate)
                .map(SkillExtraction::effectiveSkills)
                .orElse(List.of());
        List<String> required = job.getRequiredSkills() == null ? List.of() : job.getRequiredSkills();
        List<String> matched = required.stream().filter(r -> containsIgnore(candidateSkills, r)).toList();
        List<String> missing = required.stream().filter(r -> !containsIgnore(candidateSkills, r)).toList();
        int score = required.isEmpty() ? 50 : (int) Math.round(100.0 * matched.size() / required.size());
        String explanation = "Matched " + matched.size() + " of " + required.size() + " required skills.";
        String live = ai.complete(candidate,
                "Explain skill-gap results for a job seeker in 2 short paragraphs. Be specific and kind.",
                "Job: " + job.getTitle() + "\nRequired: " + required + "\nCandidate: " + candidateSkills
                        + "\nMissing: " + missing);
        if (live != null) {
            explanation = live;
        }
        SkillGapReport report = new SkillGapReport();
        report.setCandidate(candidate);
        report.setJob(job);
        report.setMatchScore(score);
        report.setMatchedSkills(matched);
        report.setMissingSkills(missing);
        report.setExplanation(explanation);
        gaps.save(report);
        return new Dtos.SkillGapView(job.getId(), job.getTitle(), score, matched, missing, explanation);
    }

    public List<Dtos.RecommendationView> recommend(User candidate) {
        List<String> skills = extractions.findFirstByCandidateOrderByUpdatedAtDesc(candidate)
                .map(SkillExtraction::effectiveSkills)
                .orElse(List.of());
        return jobs.findAll().stream()
                .filter(j -> j.getStatus() != null)
                .map(job -> {
                    List<String> required = job.getRequiredSkills() == null ? List.of() : job.getRequiredSkills();
                    long matched = required.stream().filter(r -> containsIgnore(skills, r)).count();
                    int score = required.isEmpty() ? 40 : (int) Math.round(100.0 * matched / required.size());
                    String reason = matched + "/" + required.size() + " required skills overlap: "
                            + required.stream().filter(r -> containsIgnore(skills, r)).collect(Collectors.joining(", "));
                    return new Dtos.RecommendationView(Dtos.JobView.from(job), score, reason);
                })
                .sorted((a, b) -> Integer.compare(b.matchScore(), a.matchScore()))
                .limit(6)
                .toList();
    }

    private Dtos.SkillExtractionView toView(SkillExtraction e) {
        return new Dtos.SkillExtractionView(e.getId(), e.getExtractedSkills(), e.getCorrectedSkills(),
                e.isUserCorrected(), e.getSummary(), e.getUpdatedAt());
    }

    private String resumeText(CandidateProfile profile, User candidate) {
        if (profile.getResumeStorageKey() != null) {
            return readResume(profile.getResumeStorageKey());
        }
        String stored = extractions.findFirstByCandidateOrderByUpdatedAtDesc(candidate)
                .map(SkillExtraction::getResumeText)
                .orElse("");
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        return """
                %s
                %s
                %s
                GitHub: %s
                %s
                """.formatted(
                candidate.getFullName(),
                blank(profile.getHeadline()),
                blank(profile.getLocation()),
                blank(profile.getGithubUsername()),
                blank(profile.getSummary()));
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }

    private String readResume(String key) {
        try {
            var path = storage.resolve(key);
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(path.toFile())) {
                    String extracted = new PDFTextStripper().getText(doc);
                    if (extracted == null || extracted.strip().length() < 40) {
                        throw ApiException.badRequest(
                                "This PDF has no selectable text. Export a text-based PDF, or paste the skills into the box below.");
                    }
                    return extracted;
                }
            }
            if (name.endsWith(".docx")) {
                throw ApiException.badRequest("Save the resume as PDF or TXT. DOCX text extraction is not available yet.");
            }
            return Files.readString(path);
        } catch (Exception e) {
            throw ApiException.badRequest("Could not read resume: " + e.getMessage());
        }
    }

    private Set<String> skillsListedInResume(String text) {
        Set<String> listed = new LinkedHashSet<>();
        String[] lines = text.split("\\R");
        boolean inSection = false;
        int taken = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (inSection && taken > 0) {
                    break;
                }
                continue;
            }
            if (line.matches("(?i).*(technical skills|key skills|skills|tech stack).*") && line.length() < 40) {
                inSection = true;
                String after = line.replaceAll("(?i).*(technical skills|key skills|skills|tech stack)\\s*[:\\-]?\\s*", "");
                addSkillTokens(listed, after);
                continue;
            }
            if (!inSection) {
                continue;
            }
            if (line.matches("(?i)(experience|education|projects|certifications|summary|objective).*") && line.length() < 32) {
                break;
            }
            addSkillTokens(listed, line);
            taken++;
            if (taken >= 8 || listed.size() >= 20) {
                break;
            }
        }
        return listed;
    }

    private void addSkillTokens(Set<String> listed, String line) {
        if (line.isBlank()) {
            return;
        }
        for (String token : line.split("[,;|/•·]")) {
            String skill = token.replaceAll("^[\\-\\d\\.\\)\\s]+", "").trim();
            if (skill.length() >= 2 && skill.length() <= 32 && !skill.contains("  ")) {
                listed.add(skill);
            }
        }
    }

    private Set<String> parseSkillsLine(String live) {
        Set<String> out = new LinkedHashSet<>();
        for (String line : live.split("\n")) {
            if (line.toUpperCase(Locale.ROOT).contains("SKILLS")) {
                String part = line.substring(line.indexOf(':') >= 0 ? line.indexOf(':') + 1 : 0);
                for (String token : part.split("[,;]")) {
                    String skill = token.trim();
                    if (!skill.isBlank()) {
                        out.add(skill);
                    }
                }
            }
        }
        out.addAll(SkillTaxonomy.detect(live));
        return out;
    }

    private boolean containsIgnore(List<String> hay, String needle) {
        return hay.stream().anyMatch(s -> s.equalsIgnoreCase(needle));
    }
}

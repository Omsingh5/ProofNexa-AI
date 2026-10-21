package com.proofora.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proofora.domain.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 8) String password,
            @NotBlank String fullName,
            Role role,
            String companyName
    ) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, UserView user) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ForgotPasswordResponse(String message, String resetToken) {}

    public record ResetPasswordRequest(@NotBlank String token, @NotBlank String password) {}

    public record DeactivateRequest(@NotBlank String password) {}

    public record UserView(Long id, String email, String fullName, Role role, boolean aiConsent) {
        public static UserView from(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isAiConsent());
        }
    }

    public record EducationEntry(String degree, String institution, String specialization, Integer graduationYear, String score) {}
    public record ExperienceEntry(String company, String position, String employmentType, String duration, String responsibilities) {}
    public record SkillGroup(String category, List<String> skills) {}
    public record ProjectEntry(String name, String description, String technologies, String repositoryUrl, String demoUrl) {}
    public record CertificationEntry(String name, String issuer, String date, String credentialUrl) {}
    public record AchievementEntry(String title, String detail) {}
    public record JobPostingSummary(Long id, String title, String status, String location, long applications) {}
    public record RecruitmentStats(long jobsPosted, long openJobs, long applicationsReceived) {}

    public record ProfileUpdateRequest(
            String fullName,
            String headline,
            String summary,
            String location,
            Integer yearsExperience,
            String githubUsername,
            String phone,
            String careerObjective,
            String linkedinUrl,
            String portfolioUrl,
            String websiteUrl,
            String preferredRoles,
            String preferredLocations,
            String preferredEmploymentType,
            String workMode,
            String availability,
            List<EducationEntry> education,
            List<ExperienceEntry> experience,
            List<SkillGroup> skillGroups,
            List<ProjectEntry> projects,
            List<CertificationEntry> certifications,
            List<AchievementEntry> achievements,
            String companyName,
            String companyWebsite,
            String designation,
            String department,
            String hiringSpecialization,
            String about,
            String industry,
            String companySize,
            String headquarters,
            String companyDescription,
            String jobCategories,
            String preferredTechnologies,
            String experienceLevels,
            String hiringLocations,
            Boolean aiConsent
    ) {}

    public record CandidateProfileView(
            UserView user,
            String headline,
            String summary,
            String careerObjective,
            String location,
            String phone,
            Integer yearsExperience,
            String githubUsername,
            String linkedinUrl,
            String portfolioUrl,
            String websiteUrl,
            String preferredRoles,
            String preferredLocations,
            String preferredEmploymentType,
            String workMode,
            String availability,
            List<EducationEntry> education,
            List<ExperienceEntry> experience,
            List<SkillGroup> skillGroups,
            List<ProjectEntry> projects,
            List<CertificationEntry> certifications,
            List<AchievementEntry> achievements,
            boolean hasPhoto,
            String resumeOriginalName,
            int completion
    ) {}

    public record RecruiterProfileView(
            UserView user,
            String headline,
            String location,
            String phone,
            String designation,
            String department,
            Integer yearsExperience,
            String hiringSpecialization,
            String about,
            String companyName,
            String companyWebsite,
            String industry,
            String companySize,
            String headquarters,
            String companyDescription,
            String linkedinUrl,
            String jobCategories,
            String preferredTechnologies,
            String experienceLevels,
            String hiringLocations,
            boolean hasPhoto,
            boolean hasLogo,
            List<JobPostingSummary> postings,
            RecruitmentStats stats,
            int completion
    ) {}

    public record JobRequest(
            @NotBlank String title,
            @NotBlank String description,
            String location,
            EmploymentType employmentType,
            String experienceLevel,
            List<String> requiredSkills,
            JobStatus status
    ) {}

    public record JobView(
            Long id,
            String title,
            String description,
            String location,
            EmploymentType employmentType,
            String experienceLevel,
            List<String> requiredSkills,
            JobStatus status,
            String recruiterName,
            Long recruiterId,
            Instant createdAt
    ) {
        public static JobView from(Job job) {
            return new JobView(
                    job.getId(),
                    job.getTitle(),
                    job.getDescription(),
                    job.getLocation(),
                    job.getEmploymentType(),
                    job.getExperienceLevel(),
                    job.getRequiredSkills(),
                    job.getStatus(),
                    job.getRecruiter().getFullName(),
                    job.getRecruiter().getId(),
                    job.getCreatedAt()
            );
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record ApplyRequest(String coverLetter) {}

    public record ApplicationView(
            Long id,
            Long jobId,
            String jobTitle,
            Long candidateId,
            String candidateName,
            ApplicationStatus status,
            String coverLetter,
            String recruiterFeedback,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ApplicationView from(JobApplication app) {
            return new ApplicationView(
                    app.getId(),
                    app.getJob().getId(),
                    app.getJob().getTitle(),
                    app.getCandidate().getId(),
                    app.getCandidate().getFullName(),
                    app.getStatus(),
                    app.getCoverLetter(),
                    app.getRecruiterFeedback(),
                    app.getCreatedAt(),
                    app.getUpdatedAt()
            );
        }
    }

    public record TimelineEventView(ApplicationStatus status, String note, Instant createdAt) {}

    public record StatusUpdateRequest(ApplicationStatus status, String feedback) {}

    public record SkillCorrectionRequest(List<String> skills) {}

    public record SkillExtractionView(
            Long id,
            List<String> extractedSkills,
            List<String> correctedSkills,
            boolean userCorrected,
            String summary,
            Instant updatedAt
    ) {}

    public record SkillGapView(
            Long jobId,
            String jobTitle,
            int matchScore,
            List<String> matchedSkills,
            List<String> missingSkills,
            String explanation
    ) {}

    public record RecommendationView(JobView job, int matchScore, String reason) {}

    public record RepoSubmitRequest(@NotBlank String repoUrl) {}

    public record GithubProjectView(
            Long id,
            String repoUrl,
            String owner,
            String name,
            String description,
            List<String> detectedTechnologies,
            String fileTreeSummary,
            Instant lastAnalyzedAt
    ) {
        public static GithubProjectView from(GithubProject p) {
            return new GithubProjectView(p.getId(), p.getRepoUrl(), p.getOwner(), p.getName(),
                    p.getDescription(), p.getDetectedTechnologies(), p.getFileTreeSummary(), p.getLastAnalyzedAt());
        }
    }

    public record EvidenceView(String skill, String filePath, String snippet, int confidence) {}

    public record RoadmapView(Long id, String title, String contentMarkdown, Long targetJobId, Instant createdAt) {}

    public record ChallengeView(
            Long id, String skill, String title, String prompt, ChallengeStatus status,
            String submission, String feedback, Instant createdAt, Instant completedAt
    ) {
        public static ChallengeView from(TechnicalChallenge c) {
            return new ChallengeView(c.getId(), c.getSkill(), c.getTitle(), c.getPrompt(), c.getStatus(),
                    c.getSubmission(), c.getFeedback(), c.getCreatedAt(), c.getCompletedAt());
        }
    }

    public record ChallengeSubmitRequest(@NotBlank String submission) {}

    public record QualityReportView(
            Long id,
            AnalysisMode mode,
            int architectureScore,
            int maintainabilityScore,
            int securityScore,
            int testingScore,
            int dataDesignScore,
            String architectureNotes,
            String maintainabilityNotes,
            List<String> securityFindings,
            String testingNotes,
            String dataDesignNotes,
            String recommendations,
            String evidenceSummary,
            Instant createdAt
    ) {
        public static QualityReportView from(ProjectQualityReport r) {
            return new QualityReportView(r.getId(), r.getMode(), r.getArchitectureScore(), r.getMaintainabilityScore(),
                    r.getSecurityScore(), r.getTestingScore(), r.getDataDesignScore(), r.getArchitectureNotes(),
                    r.getMaintainabilityNotes(), r.getSecurityFindings(), r.getTestingNotes(), r.getDataDesignNotes(),
                    r.getRecommendations(), r.getEvidenceSummary(), r.getCreatedAt());
        }
    }

    public record AnalyzeRequest(AnalysisMode mode) {}

    public record AssistantAskRequest(@NotBlank String message) {}

    public record NexaRequest(String message, String confirmAction) {}

    public record NexaReply(String content, String feature, String pendingAction, Instant createdAt) {}

    public record AssistantMessageView(String role, String content, String sources, Instant createdAt) {}

    public record InterviewPackView(Long id, Long jobId, String questionsMarkdown, Instant createdAt) {}

    public record JdAssistRequest(@NotBlank String notes, String title) {}

    public record AnalyticsSummary(
            long candidates,
            long recruiters,
            long openJobs,
            long applications,
            String narrative
    ) {}

    public record ConsentRequest(boolean aiConsent) {}
}

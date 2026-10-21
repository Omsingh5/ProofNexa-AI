package com.proofora.config;

import com.proofora.domain.*;
import com.proofora.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class DataSeeder {
    public static final String DEMO_PASSWORD = "Password123!";

    @Bean
    CommandLineRunner seed(
            UserRepository users,
            CandidateProfileRepository candidates,
            RecruiterProfileRepository recruiters,
            JobRepository jobs,
            JobApplicationRepository applications,
            ApplicationEventRepository events,
            KnowledgeChunkRepository chunks,
            SkillExtractionRepository extractions,
            SkillGapReportRepository gaps,
            GithubProjectRepository projects,
            SkillEvidenceRepository evidence,
            LearningRoadmapRepository roadmaps,
            TechnicalChallengeRepository challenges,
            ProjectQualityReportRepository qualityReports,
            AssistantMessageRepository messages,
            PasswordEncoder encoder
    ) {
        return args -> {
            User admin = ensureUser(users, encoder, "admin@proofora.dev", "Amina Shah", Role.ADMIN);
            User recruiter = ensureUser(users, encoder, "maya@proofora.dev", "Maya Chen", Role.RECRUITER);
            User candidate = ensureUser(users, encoder, "alex@proofora.dev", "Alex Rivera", Role.CANDIDATE);

            RecruiterProfile recruiterProfile = recruiters.findByUser(recruiter).orElseGet(RecruiterProfile::new);
            recruiterProfile.setUser(recruiter);
            recruiterProfile.setCompanyName("Northwind Labs");
            recruiterProfile.setCompanyWebsite("https://northwind.example");
            recruiters.save(recruiterProfile);

            CandidateProfile profile = candidates.findByUser(candidate).orElseGet(CandidateProfile::new);
            profile.setUser(candidate);
            profile.setHeadline("Full-stack engineer focused on Java services and React product UI");
            profile.setSummary("Four years building hiring and internal tools. I use Spring Boot, MySQL, and React, and I want project evidence to back every skill on a resume.");
            profile.setLocation("Bengaluru, India");
            profile.setYearsExperience(4);
            profile.setGithubUsername("octocat");
            candidates.save(profile);

            if (chunks.count() == 0) {
                chunk(chunks, "How applications move",
                        "Candidates apply to OPEN jobs. Recruiters move status through REVIEWING, INTERVIEW, OFFER, or REJECTED. Candidates can WITHDRAW. Every change is a timeline event.",
                        "product");
                chunk(chunks, "Skill proof",
                        "ProofNexa inspects public GitHub trees. It never executes candidate code. A skill counts as evidenced only when a file path supports it.",
                        "product");
                chunk(chunks, "Privacy",
                        "Resumes stay on the server filesystem. AI calls require consent and a daily limit. Demo accounts use the password published on the landing page.",
                        "policy");
            }

            if (!applications.findByCandidateOrderByCreatedAtDesc(candidate).isEmpty()) {
                return;
            }

            Job backend = ensureJob(jobs, recruiter, "Staff Backend Engineer",
                    "Own hiring workflows, JWT APIs, and MySQL query shape for a recruitment product. You will review application state changes and keep authorization in the service layer.",
                    "Remote", EmploymentType.FULL_TIME, "Staff",
                    List.of("Java", "Spring Boot", "Hibernate", "MySQL", "REST", "JUnit"));
            Job frontend = ensureJob(jobs, recruiter, "Frontend Product Engineer",
                    "Build candidate and recruiter dashboards in React. Accessible forms, loading states, and clear status labels matter more than decoration.",
                    "Bengaluru", EmploymentType.FULL_TIME, "Mid",
                    List.of("React", "JavaScript", "HTML5", "CSS3", "Git"));
            ensureJob(jobs, recruiter, "Platform Intern",
                    "Help with CI, Docker, and GitHub integrations. This role is open so a candidate can practice applying.",
                    "Hyderabad", EmploymentType.INTERNSHIP, "Intern",
                    List.of("Git", "GitHub", "Docker", "Java"));

            SkillExtraction extraction = new SkillExtraction();
            extraction.setCandidate(candidate);
            extraction.setResumeText("""
                    Alex Rivera
                    Bengaluru, India · alex@proofora.dev
                    Full-stack engineer, 4 years

                    Experience
                    Northwind Labs — Software Engineer, 2022–present
                    Built Spring Boot APIs for job applications, MySQL schemas, and a React hiring desk.

                    Skills
                    Java, Spring Boot, Hibernate, MySQL, REST, React, JavaScript, HTML5, CSS3, Git, JUnit

                    Education
                    B.Tech Computer Science, 2021
                    """);
            extraction.setExtractedSkills(List.of(
                    "Java", "Spring Boot", "Hibernate", "MySQL", "REST", "React", "JavaScript", "HTML5", "CSS3", "Git", "JUnit"));
            extraction.setSummary("Demo resume extraction. Java, Spring Boot, Hibernate, MySQL, REST, React, and JUnit are listed. Docker is not on the resume.");
            extraction.setUserCorrected(true);
            extraction.setCorrectedSkills(List.of(
                    "Java", "Spring Boot", "Hibernate", "MySQL", "REST", "React", "JavaScript", "HTML5", "CSS3", "Git", "JUnit"));
            extractions.save(extraction);

            SkillGapReport gap = new SkillGapReport();
            gap.setCandidate(candidate);
            gap.setJob(backend);
            gap.setMatchScore(83);
            gap.setMatchedSkills(List.of("Java", "Spring Boot", "Hibernate", "MySQL", "REST", "JUnit"));
            gap.setMissingSkills(List.of());
            gap.setExplanation("Sample comparison for the demo. The resume lists every required skill for Staff Backend Engineer. This score is matched skills divided by required skills, not a hiring decision.");
            gaps.save(gap);

            Instant appliedAt = Instant.now().minus(12, ChronoUnit.DAYS);
            JobApplication frontendApp = application(applications, frontend, candidate, ApplicationStatus.APPLIED, appliedAt,
                    "I have shipped React dashboards for internal tools and can work from Bengaluru.",
                    null);
            event(events, frontendApp, ApplicationStatus.APPLIED, "Application submitted", appliedAt);

            Instant reviewAt = Instant.now().minus(9, ChronoUnit.DAYS);
            Instant interviewAt = Instant.now().minus(2, ChronoUnit.DAYS);
            JobApplication backendApp = application(applications, backend, candidate, ApplicationStatus.INTERVIEW,
                    Instant.now().minus(10, ChronoUnit.DAYS),
                    "I have built JWT-secured Spring Boot APIs and MySQL schemas for workflow products.",
                    "Portfolio shows Spring services and tests. Please walk through authorization in the interview.");
            event(events, backendApp, ApplicationStatus.APPLIED, "Application submitted", Instant.now().minus(10, ChronoUnit.DAYS));
            event(events, backendApp, ApplicationStatus.REVIEWING, "Recruiter opened the application", reviewAt);
            event(events, backendApp, ApplicationStatus.INTERVIEW, "Interview requested for the service-layer design", interviewAt);

            GithubProject project = new GithubProject();
            project.setCandidate(candidate);
            project.setRepoUrl("https://github.com/spring-projects/spring-petclinic");
            project.setOwner("spring-projects");
            project.setName("spring-petclinic");
            project.setDefaultBranch("main");
            project.setDescription("Sample project stored for the demo. Evidence below is illustrative of a Spring Boot clinic app. Use Re-analyze after import to refresh from GitHub.");
            project.setDetectedTechnologies(List.of("Java", "Spring Boot", "Hibernate", "MySQL", "JUnit"));
            project.setFileTreeSummary("src/main/java, src/main/resources, src/test/java, pom.xml");
            project.setLastAnalyzedAt(Instant.now().minus(1, ChronoUnit.DAYS));
            projects.save(project);

            evidence(evidence, project, "Java", "src/main/java/org/springframework/samples/petclinic/owner/Owner.java",
                    "public class Owner extends Person", 80);
            evidence(evidence, project, "Spring Boot", "pom.xml",
                    "spring-boot-starter-web", 75);
            evidence(evidence, project, "Hibernate", "src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java",
                    "interface OwnerRepository extends Repository", 70);
            evidence(evidence, project, "MySQL", "src/main/resources/application.properties",
                    "spring.datasource.url=jdbc:mysql", 65);
            evidence(evidence, project, "JUnit", "src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                    "@SpringBootTest", 70);

            ProjectQualityReport report = new ProjectQualityReport();
            report.setProject(project);
            report.setMode(AnalysisMode.VALIDATION);
            report.setArchitectureScore(78);
            report.setMaintainabilityScore(74);
            report.setSecurityScore(61);
            report.setTestingScore(70);
            report.setDataDesignScore(72);
            report.setArchitectureNotes("Sample finding: controllers, services, and repositories are separated in the petclinic packages. This is a stored demo report, not a live scan.");
            report.setMaintainabilityNotes("Sample finding: domain types are small. Some controllers still mix request mapping with view concerns.");
            report.setSecurityFindings(List.of(
                    "Requires manual review: sample apps often enable a wide dev security setup.",
                    "No confirmed secret was stored in this demo report."));
            report.setTestingNotes("Sample finding: Spring Boot tests exist for owner flows. Coverage was not measured.");
            report.setDataDesignNotes("Sample finding: clinic entities use relational mappings. Do not treat this as a measured query plan.");
            report.setRecommendations("Next useful step: add an authorization test around a protected endpoint, then containerize the app if Docker is still unconfirmed.");
            report.setEvidenceSummary("Stored with the Alex Rivera demo account so Project quality opens with a report.");
            qualityReports.save(report);

            LearningRoadmap roadmap = new LearningRoadmap();
            roadmap.setCandidate(candidate);
            roadmap.setTargetJob(backend);
            roadmap.setTitle("Staff Backend Engineer — next evidence");
            roadmap.setContentMarkdown("""
                    1. Confirm JUnit on a service that changes application status.
                    2. Add one MySQL index that matches a real filter, and explain it in the README.
                    3. Docker is not on the resume. Containerize the API only after the tests exist.
                    This roadmap is sample content for the demo account.
                    """);
            roadmaps.save(roadmap);

            TechnicalChallenge done = new TechnicalChallenge();
            done.setCandidate(candidate);
            done.setSkill("JUnit");
            done.setTitle("Test an allowed application status change");
            done.setPrompt("Write a unit test that allows APPLIED → REVIEWING and rejects a jump to OFFER. Do not execute the test inside ProofNexa.");
            done.setStatus(ChallengeStatus.COMPLETED);
            done.setSubmission("Demo submission: a MockMvc test checks REVIEWING succeeds and an illegal jump returns 400.");
            done.setFeedback("Sample feedback: the submission names the transition and the expected status code. It does not prove the test was run.");
            done.setCompletedAt(Instant.now().minus(3, ChronoUnit.DAYS));
            challenges.save(done);

            TechnicalChallenge open = new TechnicalChallenge();
            open.setCandidate(candidate);
            open.setSkill("Docker");
            open.setTitle("Containerize the API without executing it here");
            open.setPrompt("Describe a Dockerfile for the Spring Boot API and a Compose service for MySQL. Paste the text. ProofNexa will not build the image.");
            open.setStatus(ChallengeStatus.OPEN);
            challenges.save(open);

            AssistantMessage question = new AssistantMessage();
            question.setUser(candidate);
            question.setRole("user");
            question.setContent("Which of my applications has moved past review?");
            messages.save(question);
            AssistantMessage answer = new AssistantMessage();
            answer.setUser(candidate);
            answer.setRole("assistant");
            answer.setSources("demo application timeline");
            answer.setContent("The Staff Backend Engineer application is in INTERVIEW. The Frontend Product Engineer application is still APPLIED. Platform Intern is open and has no application yet.");
            messages.save(answer);

            admin.getEmail();
        };
    }

    private User ensureUser(UserRepository users, PasswordEncoder encoder, String email, String name, Role role) {
        return users.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setFullName(name);
            user.setRole(role);
            user.setPasswordHash(encoder.encode(DEMO_PASSWORD));
            user.setAiConsent(true);
            user.setEnabled(true);
            return users.save(user);
        });
    }

    private Job ensureJob(JobRepository jobs, User recruiter, String title, String description, String location,
                          EmploymentType type, String level, List<String> skills) {
        return jobs.findByRecruiterOrderByCreatedAtDesc(recruiter).stream()
                .filter(job -> title.equals(job.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Job job = new Job();
                    job.setRecruiter(recruiter);
                    job.setTitle(title);
                    job.setDescription(description);
                    job.setLocation(location);
                    job.setEmploymentType(type);
                    job.setExperienceLevel(level);
                    job.setRequiredSkills(skills);
                    job.setStatus(JobStatus.OPEN);
                    return jobs.save(job);
                });
    }

    private JobApplication application(JobApplicationRepository applications, Job job, User candidate,
                                       ApplicationStatus status, Instant createdAt, String coverLetter, String feedback) {
        JobApplication app = new JobApplication();
        app.setJob(job);
        app.setCandidate(candidate);
        app.setStatus(status);
        app.setCoverLetter(coverLetter);
        app.setRecruiterFeedback(feedback);
        app.setCreatedAt(createdAt);
        app.setUpdatedAt(Instant.now());
        return applications.save(app);
    }

    private void event(ApplicationEventRepository events, JobApplication app, ApplicationStatus status, String note, Instant at) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplication(app);
        event.setStatus(status);
        event.setNote(note);
        event.setCreatedAt(at);
        events.save(event);
    }

    private void evidence(SkillEvidenceRepository evidence, GithubProject project, String skill, String path, String snippet, int confidence) {
        SkillEvidence row = new SkillEvidence();
        row.setProject(project);
        row.setSkill(skill);
        row.setFilePath(path);
        row.setSnippet(snippet);
        row.setConfidence(confidence);
        evidence.save(row);
    }

    private void chunk(KnowledgeChunkRepository chunks, String title, String body, String type) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setTitle(title);
        chunk.setBody(body);
        chunk.setSourceType(type);
        chunks.save(chunk);
    }
}

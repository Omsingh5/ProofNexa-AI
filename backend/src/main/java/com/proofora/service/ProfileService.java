package com.proofora.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofora.domain.CandidateProfile;
import com.proofora.domain.Job;
import com.proofora.domain.JobStatus;
import com.proofora.domain.RecruiterProfile;
import com.proofora.domain.Role;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.CandidateProfileRepository;
import com.proofora.repo.JobApplicationRepository;
import com.proofora.repo.JobRepository;
import com.proofora.repo.RecruiterProfileRepository;
import com.proofora.repo.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProfileService {
    private final UserRepository users;
    private final CandidateProfileRepository candidates;
    private final RecruiterProfileRepository recruiters;
    private final JobRepository jobs;
    private final JobApplicationRepository applications;
    private final FileStorageService storage;
    private final ObjectMapper mapper;

    public ProfileService(UserRepository users, CandidateProfileRepository candidates,
                          RecruiterProfileRepository recruiters, JobRepository jobs,
                          JobApplicationRepository applications, FileStorageService storage, ObjectMapper mapper) {
        this.users = users;
        this.candidates = candidates;
        this.recruiters = recruiters;
        this.jobs = jobs;
        this.applications = applications;
        this.storage = storage;
        this.mapper = mapper;
    }

    public Object me(User user) {
        if (user.getRole() == Role.CANDIDATE) {
            return candidateView(user, candidates.findByUser(user).orElseThrow());
        }
        if (user.getRole() == Role.RECRUITER) {
            return recruiterView(user, recruiters.findByUser(user).orElseThrow());
        }
        return Dtos.UserView.from(user);
    }

    @Transactional
    public Object update(User user, Dtos.ProfileUpdateRequest request) {
        if (request.fullName() != null) {
            String name = ProfileChecks.text(request.fullName(), 120);
            if (name.isEmpty()) {
                throw ApiException.badRequest("Full name is required.");
            }
            user.setFullName(name);
        }
        if (request.aiConsent() != null) {
            user.setAiConsent(request.aiConsent());
        }
        users.save(user);
        if (user.getRole() == Role.CANDIDATE) {
            saveCandidate(candidates.findByUser(user).orElseThrow(), request);
        }
        if (user.getRole() == Role.RECRUITER || user.getRole() == Role.ADMIN) {
            if (user.getRole() == Role.RECRUITER) {
                saveRecruiter(recruiters.findByUser(user).orElseThrow(), request);
            }
        }
        return me(user);
    }

    @Transactional
    public void uploadResume(User user, MultipartFile file) {
        if (user.getRole() != Role.CANDIDATE) {
            throw ApiException.forbidden("Only candidates upload resumes");
        }
        CandidateProfile profile = candidates.findByUser(user).orElseThrow();
        String key = storage.storeResume(user.getId(), file);
        profile.setResumeStorageKey(key);
        profile.setResumeOriginalName(file.getOriginalFilename());
        candidates.save(profile);
    }

    @Transactional
    public void uploadPhoto(User user, MultipartFile file) {
        if (user.getRole() == Role.CANDIDATE) {
            CandidateProfile profile = candidates.findByUser(user).orElseThrow();
            profile.setPhotoStorageKey(storage.storeImage(user.getId(), "photos", file));
            candidates.save(profile);
            return;
        }
        if (user.getRole() == Role.RECRUITER) {
            RecruiterProfile profile = recruiters.findByUser(user).orElseThrow();
            profile.setPhotoStorageKey(storage.storeImage(user.getId(), "photos", file));
            recruiters.save(profile);
            return;
        }
        throw ApiException.forbidden("This account cannot store a profile photo.");
    }

    @Transactional
    public void uploadLogo(User user, MultipartFile file) {
        if (user.getRole() != Role.RECRUITER) {
            throw ApiException.forbidden("Only a recruiter can upload a company logo.");
        }
        RecruiterProfile profile = recruiters.findByUser(user).orElseThrow();
        profile.setLogoStorageKey(storage.storeImage(user.getId(), "logos", file));
        recruiters.save(profile);
    }

    public ResponseEntity<Resource> ownImage(User user, boolean logo) {
        String key = imageKey(user, logo);
        if (key == null || key.isBlank()) {
            throw ApiException.notFound("No image uploaded");
        }
        MediaType type = key.endsWith(".png") ? MediaType.IMAGE_PNG
                : key.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(storage.resolve(key)));
    }

    public ResponseEntity<Resource> downloadResume(User requester, Long candidateUserId) {
        User candidate = users.findById(candidateUserId).orElseThrow(() -> ApiException.notFound("User not found"));
        boolean self = requester.getId().equals(candidateUserId);
        boolean privileged = requester.getRole() == Role.RECRUITER || requester.getRole() == Role.ADMIN;
        if (!self && !privileged) {
            throw ApiException.forbidden("Resume access denied");
        }
        CandidateProfile profile = candidates.findByUser(candidate)
                .orElseThrow(() -> ApiException.notFound("Profile not found"));
        if (profile.getResumeStorageKey() == null) {
            throw ApiException.notFound("No resume uploaded");
        }
        Resource resource = new FileSystemResource(storage.resolve(profile.getResumeStorageKey()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + profile.getResumeOriginalName() + "\"")
                .body(resource);
    }

    private void saveCandidate(CandidateProfile profile, Dtos.ProfileUpdateRequest request) {
        if (request.headline() != null) profile.setHeadline(ProfileChecks.text(request.headline(), 160));
        if (request.summary() != null) profile.setSummary(ProfileChecks.text(request.summary(), 4000));
        if (request.careerObjective() != null) profile.setCareerObjective(ProfileChecks.text(request.careerObjective(), 2000));
        if (request.location() != null) profile.setLocation(ProfileChecks.text(request.location(), 120));
        if (request.phone() != null) profile.setPhone(ProfileChecks.phone(request.phone()));
        if (request.yearsExperience() != null) profile.setYearsExperience(ProfileChecks.years(request.yearsExperience()));
        if (request.githubUsername() != null) profile.setGithubUsername(ProfileChecks.text(request.githubUsername(), 80));
        if (request.linkedinUrl() != null) profile.setLinkedinUrl(ProfileChecks.optionalUrl(request.linkedinUrl(), "LinkedIn"));
        if (request.portfolioUrl() != null) profile.setPortfolioUrl(ProfileChecks.optionalUrl(request.portfolioUrl(), "Portfolio"));
        if (request.websiteUrl() != null) profile.setWebsiteUrl(ProfileChecks.optionalUrl(request.websiteUrl(), "Website"));
        if (request.preferredRoles() != null) profile.setPreferredRoles(ProfileChecks.text(request.preferredRoles(), 300));
        if (request.preferredLocations() != null) profile.setPreferredLocations(ProfileChecks.text(request.preferredLocations(), 300));
        if (request.preferredEmploymentType() != null) profile.setPreferredEmploymentType(ProfileChecks.text(request.preferredEmploymentType(), 40));
        if (request.workMode() != null) profile.setWorkMode(ProfileChecks.text(request.workMode(), 40));
        if (request.availability() != null) profile.setAvailability(ProfileChecks.text(request.availability(), 80));
        if (request.education() != null) profile.setEducationJson(write(cleanEducation(request.education())));
        if (request.experience() != null) profile.setExperienceJson(write(cleanExperience(request.experience())));
        if (request.skillGroups() != null) profile.setSkillGroupsJson(write(cleanSkills(request.skillGroups())));
        if (request.projects() != null) profile.setProjectsJson(write(cleanProjects(request.projects())));
        if (request.certifications() != null) profile.setCertificationsJson(write(cleanCertificates(request.certifications())));
        if (request.achievements() != null) profile.setAchievementsJson(write(cleanAchievements(request.achievements())));
        candidates.save(profile);
    }

    private void saveRecruiter(RecruiterProfile profile, Dtos.ProfileUpdateRequest request) {
        if (request.headline() != null) profile.setHeadline(ProfileChecks.text(request.headline(), 160));
        if (request.location() != null) profile.setLocation(ProfileChecks.text(request.location(), 120));
        if (request.phone() != null) profile.setPhone(ProfileChecks.phone(request.phone()));
        if (request.designation() != null) profile.setDesignation(ProfileChecks.text(request.designation(), 120));
        if (request.department() != null) profile.setDepartment(ProfileChecks.text(request.department(), 120));
        if (request.yearsExperience() != null) profile.setYearsExperience(ProfileChecks.years(request.yearsExperience()));
        if (request.hiringSpecialization() != null) profile.setHiringSpecialization(ProfileChecks.text(request.hiringSpecialization(), 200));
        if (request.about() != null) profile.setAbout(ProfileChecks.text(request.about(), 2000));
        if (request.companyName() != null) profile.setCompanyName(ProfileChecks.text(request.companyName(), 160));
        if (request.companyWebsite() != null) profile.setCompanyWebsite(ProfileChecks.optionalUrl(request.companyWebsite(), "Company website"));
        if (request.industry() != null) profile.setIndustry(ProfileChecks.text(request.industry(), 120));
        if (request.companySize() != null) profile.setCompanySize(ProfileChecks.text(request.companySize(), 40));
        if (request.headquarters() != null) profile.setHeadquarters(ProfileChecks.text(request.headquarters(), 120));
        if (request.companyDescription() != null) profile.setCompanyDescription(ProfileChecks.text(request.companyDescription(), 4000));
        if (request.linkedinUrl() != null) profile.setLinkedinUrl(ProfileChecks.optionalUrl(request.linkedinUrl(), "LinkedIn"));
        if (request.jobCategories() != null) profile.setJobCategories(ProfileChecks.text(request.jobCategories(), 300));
        if (request.preferredTechnologies() != null) profile.setPreferredTechnologies(ProfileChecks.text(request.preferredTechnologies(), 300));
        if (request.experienceLevels() != null) profile.setExperienceLevels(ProfileChecks.text(request.experienceLevels(), 200));
        if (request.hiringLocations() != null) profile.setHiringLocations(ProfileChecks.text(request.hiringLocations(), 300));
        recruiters.save(profile);
    }

    private Dtos.CandidateProfileView candidateView(User user, CandidateProfile profile) {
        List<Dtos.EducationEntry> education = read(profile.getEducationJson(), new TypeReference<>() {});
        List<Dtos.ExperienceEntry> experience = read(profile.getExperienceJson(), new TypeReference<>() {});
        List<Dtos.SkillGroup> skills = read(profile.getSkillGroupsJson(), new TypeReference<>() {});
        List<Dtos.ProjectEntry> projects = read(profile.getProjectsJson(), new TypeReference<>() {});
        List<Dtos.CertificationEntry> certificates = read(profile.getCertificationsJson(), new TypeReference<>() {});
        List<Dtos.AchievementEntry> achievements = read(profile.getAchievementsJson(), new TypeReference<>() {});
        boolean photo = present(profile.getPhotoStorageKey());
        boolean resume = present(profile.getResumeOriginalName());
        int completion = ProfileChecks.completion(ProfileChecks.filled(
                present(user.getFullName()), present(profile.getHeadline()), present(profile.getLocation()), present(user.getEmail()),
                present(profile.getPhone()), photo, present(profile.getSummary()), present(profile.getCareerObjective()),
                !education.isEmpty(), !experience.isEmpty(), !skills.isEmpty(), !projects.isEmpty(),
                !certificates.isEmpty(), !achievements.isEmpty(), resume,
                present(profile.getLinkedinUrl()) || present(profile.getGithubUsername()),
                present(profile.getPortfolioUrl()) || present(profile.getWebsiteUrl()),
                present(profile.getPreferredRoles()) && present(profile.getWorkMode()) && present(profile.getAvailability())
        ), 18);
        return new Dtos.CandidateProfileView(
                Dtos.UserView.from(user), profile.getHeadline(), profile.getSummary(), profile.getCareerObjective(),
                profile.getLocation(), profile.getPhone(), profile.getYearsExperience(), profile.getGithubUsername(),
                profile.getLinkedinUrl(), profile.getPortfolioUrl(), profile.getWebsiteUrl(),
                profile.getPreferredRoles(), profile.getPreferredLocations(), profile.getPreferredEmploymentType(),
                profile.getWorkMode(), profile.getAvailability(), education, experience, skills, projects,
                certificates, achievements, photo, profile.getResumeOriginalName(), completion);
    }

    private Dtos.RecruiterProfileView recruiterView(User user, RecruiterProfile profile) {
        List<Job> owned = jobs.findByRecruiterOrderByCreatedAtDesc(user);
        List<Dtos.JobPostingSummary> postings = owned.stream()
                .map(job -> new Dtos.JobPostingSummary(job.getId(), job.getTitle(),
                        job.getStatus() == null ? "" : job.getStatus().name(), job.getLocation(),
                        applications.findByJobOrderByCreatedAtDesc(job).size()))
                .toList();
        long open = owned.stream().filter(job -> job.getStatus() == JobStatus.OPEN).count();
        Dtos.RecruitmentStats stats = new Dtos.RecruitmentStats(owned.size(), open, applications.countByJobRecruiter(user));
        boolean photo = present(profile.getPhotoStorageKey());
        boolean logo = present(profile.getLogoStorageKey());
        int completion = ProfileChecks.completion(ProfileChecks.filled(
                present(user.getFullName()), present(profile.getHeadline()), present(profile.getLocation()), present(profile.getPhone()), photo,
                present(profile.getDesignation()), present(profile.getDepartment()), profile.getYearsExperience() != null,
                present(profile.getHiringSpecialization()), present(profile.getAbout()), present(profile.getCompanyName()), logo,
                present(profile.getIndustry()), present(profile.getCompanySize()), present(profile.getHeadquarters()),
                present(profile.getCompanyWebsite()), present(profile.getCompanyDescription()), present(profile.getLinkedinUrl()),
                present(profile.getJobCategories()) && present(profile.getPreferredTechnologies()) && present(profile.getHiringLocations())
        ), 19);
        return new Dtos.RecruiterProfileView(
                Dtos.UserView.from(user), profile.getHeadline(), profile.getLocation(), profile.getPhone(),
                profile.getDesignation(), profile.getDepartment(), profile.getYearsExperience(), profile.getHiringSpecialization(),
                profile.getAbout(), profile.getCompanyName(), profile.getCompanyWebsite(), profile.getIndustry(),
                profile.getCompanySize(), profile.getHeadquarters(), profile.getCompanyDescription(), profile.getLinkedinUrl(),
                profile.getJobCategories(), profile.getPreferredTechnologies(), profile.getExperienceLevels(),
                profile.getHiringLocations(), photo, logo, postings, stats, completion);
    }

    private String imageKey(User user, boolean logo) {
        if (logo) {
            if (user.getRole() != Role.RECRUITER) {
                throw ApiException.forbidden("Only a recruiter has a company logo.");
            }
            return recruiters.findByUser(user).orElseThrow().getLogoStorageKey();
        }
        if (user.getRole() == Role.CANDIDATE) {
            return candidates.findByUser(user).orElseThrow().getPhotoStorageKey();
        }
        if (user.getRole() == Role.RECRUITER) {
            return recruiters.findByUser(user).orElseThrow().getPhotoStorageKey();
        }
        throw ApiException.forbidden("This account has no profile image.");
    }

    private List<Dtos.EducationEntry> cleanEducation(List<Dtos.EducationEntry> rows) {
        return ProfileChecks.limit(rows).stream()
                .map(row -> new Dtos.EducationEntry(
                        ProfileChecks.text(row.degree(), 120),
                        ProfileChecks.text(row.institution(), 160),
                        ProfileChecks.text(row.specialization(), 120),
                        ProfileChecks.year(row.graduationYear()),
                        ProfileChecks.text(row.score(), 20)))
                .filter(row -> present(row.degree()) || present(row.institution()))
                .toList();
    }

    private List<Dtos.ExperienceEntry> cleanExperience(List<Dtos.ExperienceEntry> rows) {
        return ProfileChecks.limit(rows).stream()
                .map(row -> new Dtos.ExperienceEntry(
                        ProfileChecks.text(row.company(), 160),
                        ProfileChecks.text(row.position(), 120),
                        ProfileChecks.text(row.employmentType(), 40),
                        ProfileChecks.text(row.duration(), 60),
                        ProfileChecks.text(row.responsibilities(), 2000)))
                .filter(row -> present(row.company()) || present(row.position()))
                .toList();
    }

    private List<Dtos.SkillGroup> cleanSkills(List<Dtos.SkillGroup> rows) {
        return ProfileChecks.limit(rows).stream()
                .map(row -> new Dtos.SkillGroup(ProfileChecks.text(row.category(), 80),
                        row.skills() == null ? List.of() : row.skills().stream().map(skill -> ProfileChecks.text(skill, 40)).filter(this::present).limit(20).toList()))
                .filter(row -> present(row.category()) && !row.skills().isEmpty())
                .toList();
    }

    private List<Dtos.ProjectEntry> cleanProjects(List<Dtos.ProjectEntry> rows) {
        return ProfileChecks.limit(rows).stream()
                .map(row -> new Dtos.ProjectEntry(
                        ProfileChecks.text(row.name(), 120),
                        ProfileChecks.text(row.description(), 1000),
                        ProfileChecks.text(row.technologies(), 200),
                        ProfileChecks.optionalUrl(row.repositoryUrl(), "Repository"),
                        ProfileChecks.optionalUrl(row.demoUrl(), "Live demo")))
                .filter(row -> present(row.name()))
                .toList();
    }

    private List<Dtos.CertificationEntry> cleanCertificates(List<Dtos.CertificationEntry> rows) {
        return ProfileChecks.limit(rows).stream()
                .map(row -> new Dtos.CertificationEntry(
                        ProfileChecks.text(row.name(), 160),
                        ProfileChecks.text(row.issuer(), 160),
                        ProfileChecks.text(row.date(), 40),
                        ProfileChecks.optionalUrl(row.credentialUrl(), "Credential")))
                .filter(row -> present(row.name()))
                .toList();
    }

    private List<Dtos.AchievementEntry> cleanAchievements(List<Dtos.AchievementEntry> rows) {
        return ProfileChecks.limit(rows).stream()
                .map(row -> new Dtos.AchievementEntry(ProfileChecks.text(row.title(), 160), ProfileChecks.text(row.detail(), 500)))
                .filter(row -> present(row.title()))
                .toList();
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw ApiException.badRequest("Could not save that profile section.");
        }
    }

    private <T> List<T> read(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}

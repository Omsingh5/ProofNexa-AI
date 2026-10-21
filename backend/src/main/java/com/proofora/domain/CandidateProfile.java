package com.proofora.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String headline;
    @Column(length = 4000)
    private String summary;
    private String location;
    private Integer yearsExperience;
    private String githubUsername;
    private String phone;
    @Column(length = 2000)
    private String careerObjective;
    private String photoStorageKey;
    private String linkedinUrl;
    private String portfolioUrl;
    private String websiteUrl;
    private String preferredRoles;
    private String preferredLocations;
    private String preferredEmploymentType;
    private String workMode;
    private String availability;
    @Column(length = 8000)
    private String educationJson;
    @Column(length = 8000)
    private String experienceJson;
    @Column(length = 4000)
    private String skillGroupsJson;
    @Column(length = 8000)
    private String projectsJson;
    @Column(length = 4000)
    private String certificationsJson;
    @Column(length = 4000)
    private String achievementsJson;
    private String resumeStorageKey;
    private String resumeOriginalName;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }
    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCareerObjective() { return careerObjective; }
    public void setCareerObjective(String careerObjective) { this.careerObjective = careerObjective; }
    public String getPhotoStorageKey() { return photoStorageKey; }
    public void setPhotoStorageKey(String photoStorageKey) { this.photoStorageKey = photoStorageKey; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getPreferredRoles() { return preferredRoles; }
    public void setPreferredRoles(String preferredRoles) { this.preferredRoles = preferredRoles; }
    public String getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(String preferredLocations) { this.preferredLocations = preferredLocations; }
    public String getPreferredEmploymentType() { return preferredEmploymentType; }
    public void setPreferredEmploymentType(String preferredEmploymentType) { this.preferredEmploymentType = preferredEmploymentType; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
    public String getEducationJson() { return educationJson; }
    public void setEducationJson(String educationJson) { this.educationJson = educationJson; }
    public String getExperienceJson() { return experienceJson; }
    public void setExperienceJson(String experienceJson) { this.experienceJson = experienceJson; }
    public String getSkillGroupsJson() { return skillGroupsJson; }
    public void setSkillGroupsJson(String skillGroupsJson) { this.skillGroupsJson = skillGroupsJson; }
    public String getProjectsJson() { return projectsJson; }
    public void setProjectsJson(String projectsJson) { this.projectsJson = projectsJson; }
    public String getCertificationsJson() { return certificationsJson; }
    public void setCertificationsJson(String certificationsJson) { this.certificationsJson = certificationsJson; }
    public String getAchievementsJson() { return achievementsJson; }
    public void setAchievementsJson(String achievementsJson) { this.achievementsJson = achievementsJson; }
    public String getResumeStorageKey() { return resumeStorageKey; }
    public void setResumeStorageKey(String resumeStorageKey) { this.resumeStorageKey = resumeStorageKey; }
    public String getResumeOriginalName() { return resumeOriginalName; }
    public void setResumeOriginalName(String resumeOriginalName) { this.resumeOriginalName = resumeOriginalName; }
}

package com.proofora.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "recruiter_profiles")
public class RecruiterProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String companyName;
    private String companyWebsite;
    private String headline;
    private String location;
    private String phone;
    private String designation;
    private String department;
    private Integer yearsExperience;
    private String hiringSpecialization;
    @Column(length = 2000)
    private String about;
    private String industry;
    private String companySize;
    private String headquarters;
    @Column(length = 4000)
    private String companyDescription;
    private String linkedinUrl;
    private String photoStorageKey;
    private String logoStorageKey;
    private String jobCategories;
    private String preferredTechnologies;
    private String experienceLevels;
    private String hiringLocations;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }
    public String getHiringSpecialization() { return hiringSpecialization; }
    public void setHiringSpecialization(String hiringSpecialization) { this.hiringSpecialization = hiringSpecialization; }
    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }
    public String getHeadquarters() { return headquarters; }
    public void setHeadquarters(String headquarters) { this.headquarters = headquarters; }
    public String getCompanyDescription() { return companyDescription; }
    public void setCompanyDescription(String companyDescription) { this.companyDescription = companyDescription; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getPhotoStorageKey() { return photoStorageKey; }
    public void setPhotoStorageKey(String photoStorageKey) { this.photoStorageKey = photoStorageKey; }
    public String getLogoStorageKey() { return logoStorageKey; }
    public void setLogoStorageKey(String logoStorageKey) { this.logoStorageKey = logoStorageKey; }
    public String getJobCategories() { return jobCategories; }
    public void setJobCategories(String jobCategories) { this.jobCategories = jobCategories; }
    public String getPreferredTechnologies() { return preferredTechnologies; }
    public void setPreferredTechnologies(String preferredTechnologies) { this.preferredTechnologies = preferredTechnologies; }
    public String getExperienceLevels() { return experienceLevels; }
    public void setExperienceLevels(String experienceLevels) { this.experienceLevels = experienceLevels; }
    public String getHiringLocations() { return hiringLocations; }
    public void setHiringLocations(String hiringLocations) { this.hiringLocations = hiringLocations; }
}

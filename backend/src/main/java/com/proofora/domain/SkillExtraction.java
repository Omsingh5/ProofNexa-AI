package com.proofora.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_extractions")
public class SkillExtraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User candidate;

    @Column(length = 16000)
    private String resumeText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> extractedSkills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> correctedSkills = new ArrayList<>();

    @Column(length = 8000)
    private String summary;

    private boolean userCorrected;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public User getCandidate() { return candidate; }
    public void setCandidate(User candidate) { this.candidate = candidate; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public List<String> getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(List<String> extractedSkills) { this.extractedSkills = extractedSkills; }
    public List<String> getCorrectedSkills() { return correctedSkills; }
    public void setCorrectedSkills(List<String> correctedSkills) { this.correctedSkills = correctedSkills; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public boolean isUserCorrected() { return userCorrected; }
    public void setUserCorrected(boolean userCorrected) { this.userCorrected = userCorrected; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<String> effectiveSkills() {
        if (userCorrected && correctedSkills != null && !correctedSkills.isEmpty()) {
            return correctedSkills;
        }
        return extractedSkills;
    }
}

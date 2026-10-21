package com.proofora.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_gap_reports")
public class SkillGapReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User candidate;

    @ManyToOne(optional = false)
    private Job job;

    private int matchScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> matchedSkills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> missingSkills = new ArrayList<>();

    @Column(length = 4000)
    private String explanation;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getCandidate() { return candidate; }
    public void setCandidate(User candidate) { this.candidate = candidate; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public Instant getCreatedAt() { return createdAt; }
}

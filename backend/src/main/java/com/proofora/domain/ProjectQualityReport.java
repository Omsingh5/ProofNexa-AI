package com.proofora.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_quality_reports")
public class ProjectQualityReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private GithubProject project;

    @Enumerated(EnumType.STRING)
    private AnalysisMode mode = AnalysisMode.VALIDATION;

    private int architectureScore;
    private int maintainabilityScore;
    private int securityScore;
    private int testingScore;
    private int dataDesignScore;

    @Column(length = 4000)
    private String architectureNotes;
    @Column(length = 4000)
    private String maintainabilityNotes;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> securityFindings = new ArrayList<>();
    @Column(length = 2000)
    private String testingNotes;
    @Column(length = 2000)
    private String dataDesignNotes;
    @Column(length = 8000)
    private String recommendations;
    @Column(length = 8000)
    private String evidenceSummary;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public GithubProject getProject() { return project; }
    public void setProject(GithubProject project) { this.project = project; }
    public AnalysisMode getMode() { return mode; }
    public void setMode(AnalysisMode mode) { this.mode = mode; }
    public int getArchitectureScore() { return architectureScore; }
    public void setArchitectureScore(int architectureScore) { this.architectureScore = architectureScore; }
    public int getMaintainabilityScore() { return maintainabilityScore; }
    public void setMaintainabilityScore(int maintainabilityScore) { this.maintainabilityScore = maintainabilityScore; }
    public int getSecurityScore() { return securityScore; }
    public void setSecurityScore(int securityScore) { this.securityScore = securityScore; }
    public int getTestingScore() { return testingScore; }
    public void setTestingScore(int testingScore) { this.testingScore = testingScore; }
    public int getDataDesignScore() { return dataDesignScore; }
    public void setDataDesignScore(int dataDesignScore) { this.dataDesignScore = dataDesignScore; }
    public String getArchitectureNotes() { return architectureNotes; }
    public void setArchitectureNotes(String architectureNotes) { this.architectureNotes = architectureNotes; }
    public String getMaintainabilityNotes() { return maintainabilityNotes; }
    public void setMaintainabilityNotes(String maintainabilityNotes) { this.maintainabilityNotes = maintainabilityNotes; }
    public List<String> getSecurityFindings() { return securityFindings; }
    public void setSecurityFindings(List<String> securityFindings) { this.securityFindings = securityFindings; }
    public String getTestingNotes() { return testingNotes; }
    public void setTestingNotes(String testingNotes) { this.testingNotes = testingNotes; }
    public String getDataDesignNotes() { return dataDesignNotes; }
    public void setDataDesignNotes(String dataDesignNotes) { this.dataDesignNotes = dataDesignNotes; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(String evidenceSummary) { this.evidenceSummary = evidenceSummary; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.proofora.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "github_projects")
public class GithubProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User candidate;

    @Column(nullable = false)
    private String repoUrl;

    private String owner;
    private String name;
    private String defaultBranch;

    @Column(length = 4000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> detectedTechnologies = new ArrayList<>();

    @Column(length = 16000)
    private String fileTreeSummary;

    private Instant lastAnalyzedAt;

    public Long getId() { return id; }
    public User getCandidate() { return candidate; }
    public void setCandidate(User candidate) { this.candidate = candidate; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getDetectedTechnologies() { return detectedTechnologies; }
    public void setDetectedTechnologies(List<String> detectedTechnologies) { this.detectedTechnologies = detectedTechnologies; }
    public String getFileTreeSummary() { return fileTreeSummary; }
    public void setFileTreeSummary(String fileTreeSummary) { this.fileTreeSummary = fileTreeSummary; }
    public Instant getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(Instant lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; }
}

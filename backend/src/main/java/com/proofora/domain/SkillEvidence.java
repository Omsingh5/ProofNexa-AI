package com.proofora.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "skill_evidence")
public class SkillEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private GithubProject project;

    private String skill;
    private String filePath;
    @Column(length = 4000)
    private String snippet;
    private int confidence;

    public Long getId() { return id; }
    public GithubProject getProject() { return project; }
    public void setProject(GithubProject project) { this.project = project; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
}

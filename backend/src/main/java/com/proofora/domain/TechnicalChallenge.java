package com.proofora.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "technical_challenges")
public class TechnicalChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User candidate;

    private String skill;
    private String title;
    @Column(length = 8000)
    private String prompt;
    @Enumerated(EnumType.STRING)
    private ChallengeStatus status = ChallengeStatus.OPEN;
    @Column(length = 8000)
    private String submission;
    @Column(length = 4000)
    private String feedback;
    private Instant createdAt = Instant.now();
    private Instant completedAt;

    public Long getId() { return id; }
    public User getCandidate() { return candidate; }
    public void setCandidate(User candidate) { this.candidate = candidate; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public ChallengeStatus getStatus() { return status; }
    public void setStatus(ChallengeStatus status) { this.status = status; }
    public String getSubmission() { return submission; }
    public void setSubmission(String submission) { this.submission = submission; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

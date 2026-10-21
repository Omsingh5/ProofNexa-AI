package com.proofora.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "learning_roadmaps")
public class LearningRoadmap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User candidate;

    @ManyToOne
    private Job targetJob;

    @Column(nullable = false)
    private String title;

    @Column(length = 12000)
    private String contentMarkdown;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getCandidate() { return candidate; }
    public void setCandidate(User candidate) { this.candidate = candidate; }
    public Job getTargetJob() { return targetJob; }
    public void setTargetJob(Job targetJob) { this.targetJob = targetJob; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public Instant getCreatedAt() { return createdAt; }
}

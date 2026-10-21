package com.proofora.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "interview_packs")
public class InterviewPack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Job job;

    @ManyToOne
    private JobApplication application;

    @Column(length = 12000)
    private String questionsMarkdown;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public JobApplication getApplication() { return application; }
    public void setApplication(JobApplication application) { this.application = application; }
    public String getQuestionsMarkdown() { return questionsMarkdown; }
    public void setQuestionsMarkdown(String questionsMarkdown) { this.questionsMarkdown = questionsMarkdown; }
    public Instant getCreatedAt() { return createdAt; }
}

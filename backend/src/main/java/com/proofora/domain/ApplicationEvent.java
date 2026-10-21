package com.proofora.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "application_events")
public class ApplicationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column(length = 2000)
    private String note;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public JobApplication getApplication() { return application; }
    public void setApplication(JobApplication application) { this.application = application; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

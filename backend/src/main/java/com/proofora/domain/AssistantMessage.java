package com.proofora.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "assistant_messages")
public class AssistantMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private String role;
    @Column(length = 8000)
    private String content;
    @Column(length = 2000)
    private String sources;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }
    public Instant getCreatedAt() { return createdAt; }
}

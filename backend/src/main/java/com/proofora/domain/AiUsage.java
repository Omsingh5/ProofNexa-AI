package com.proofora.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "ai_usage", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "usageDate"}))
public class AiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private LocalDate usageDate = LocalDate.now();
    private int callCount;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }
    public int getCallCount() { return callCount; }
    public void setCallCount(int callCount) { this.callCount = callCount; }
}

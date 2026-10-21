package com.proofora.service;

import com.proofora.ai.AiGateway;
import com.proofora.domain.JobStatus;
import com.proofora.domain.Role;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.repo.JobApplicationRepository;
import com.proofora.repo.JobRepository;
import com.proofora.repo.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final UserRepository users;
    private final JobRepository jobs;
    private final JobApplicationRepository applications;
    private final AiGateway ai;

    public AnalyticsService(UserRepository users, JobRepository jobs, JobApplicationRepository applications, AiGateway ai) {
        this.users = users;
        this.jobs = jobs;
        this.applications = applications;
        this.ai = ai;
    }

    public Dtos.AnalyticsSummary summarize(User actor) {
        long candidates = users.countByRole(Role.CANDIDATE);
        long recruiters = users.countByRole(Role.RECRUITER);
        long openJobs = jobs.countByStatus(JobStatus.OPEN);
        long apps = applications.count();
        String narrative = "Proofora currently tracks " + candidates + " candidates, " + recruiters
                + " recruiters, " + openJobs + " open roles, and " + apps + " applications.";
        String live = ai.complete(actor,
                "Write a 4-sentence recruiting analytics briefing from the numbers. No hype.",
                narrative);
        return new Dtos.AnalyticsSummary(candidates, recruiters, openJobs, apps, live == null ? narrative : live);
    }
}

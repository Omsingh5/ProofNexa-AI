package com.proofora.service;

import com.proofora.domain.*;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.ApplicationEventRepository;
import com.proofora.repo.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ApplicationService {
    private final JobApplicationRepository applications;
    private final ApplicationEventRepository events;
    private final JobService jobs;
    private final AuditService audit;

    public ApplicationService(JobApplicationRepository applications, ApplicationEventRepository events,
                              JobService jobs, AuditService audit) {
        this.applications = applications;
        this.events = events;
        this.jobs = jobs;
        this.audit = audit;
    }

    @Transactional
    public Dtos.ApplicationView apply(User candidate, Long jobId, Dtos.ApplyRequest request) {
        if (candidate.getRole() != Role.CANDIDATE) {
            throw ApiException.forbidden("Only candidates can apply");
        }
        Job job = jobs.entity(jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw ApiException.badRequest("This job is not open");
        }
        if (applications.existsByJobAndCandidate(job, candidate)) {
            throw ApiException.conflict("You already applied to this job");
        }
        JobApplication app = new JobApplication();
        app.setJob(job);
        app.setCandidate(candidate);
        app.setCoverLetter(request.coverLetter());
        applications.save(app);
        addEvent(app, ApplicationStatus.APPLIED, "Application submitted");
        audit.record(candidate, "APPLY", "JobApplication", app.getId(), job.getTitle());
        return Dtos.ApplicationView.from(app);
    }

    public List<Dtos.ApplicationView> mine(User candidate) {
        return applications.findByCandidateOrderByCreatedAtDesc(candidate).stream()
                .map(Dtos.ApplicationView::from).toList();
    }

    public List<Dtos.ApplicationView> forJob(User recruiter, Long jobId) {
        Job job = jobs.entity(jobId);
        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Not your job posting");
        }
        return applications.findByJobOrderByCreatedAtDesc(job).stream().map(Dtos.ApplicationView::from).toList();
    }

    public List<Dtos.TimelineEventView> timeline(User user, Long applicationId) {
        JobApplication app = loadVisible(user, applicationId);
        return events.findByApplicationOrderByCreatedAtAsc(app).stream()
                .map(e -> new Dtos.TimelineEventView(e.getStatus(), e.getNote(), e.getCreatedAt()))
                .toList();
    }

    @Transactional
    public Dtos.ApplicationView updateStatus(User recruiter, Long applicationId, Dtos.StatusUpdateRequest request) {
        JobApplication app = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        if (!app.getJob().getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Not your job posting");
        }
        app.setStatus(request.status());
        app.setRecruiterFeedback(request.feedback());
        app.setUpdatedAt(Instant.now());
        addEvent(app, request.status(), request.feedback() == null ? "Status updated" : request.feedback());
        audit.record(recruiter, "APPLICATION_STATUS", "JobApplication", app.getId(), request.status().name());
        return Dtos.ApplicationView.from(app);
    }

    @Transactional
    public Dtos.ApplicationView withdraw(User candidate, Long applicationId) {
        JobApplication app = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        if (!app.getCandidate().getId().equals(candidate.getId())) {
            throw ApiException.forbidden("Not your application");
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        app.setUpdatedAt(Instant.now());
        addEvent(app, ApplicationStatus.WITHDRAWN, "Candidate withdrew");
        return Dtos.ApplicationView.from(app);
    }

    private JobApplication loadVisible(User user, Long applicationId) {
        JobApplication app = applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application not found"));
        boolean owner = app.getCandidate().getId().equals(user.getId());
        boolean recruiter = app.getJob().getRecruiter().getId().equals(user.getId());
        if (!owner && !recruiter && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Not allowed to view this application");
        }
        return app;
    }

    private void addEvent(JobApplication app, ApplicationStatus status, String note) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplication(app);
        event.setStatus(status);
        event.setNote(note);
        events.save(event);
    }
}

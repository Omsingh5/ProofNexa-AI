package com.proofora.service;

import com.proofora.domain.*;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.JobRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {
    private final JobRepository jobs;
    private final AuditService audit;

    public JobService(JobRepository jobs, AuditService audit) {
        this.jobs = jobs;
        this.audit = audit;
    }

    public Dtos.PageResponse<Dtos.JobView> search(String q, String location, EmploymentType type, int page, int size) {
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), JobStatus.OPEN));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("employmentType"), type));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Job> result = jobs.findAll(spec, PageRequest.of(page, size));
        return new Dtos.PageResponse<>(
                result.map(Dtos.JobView::from).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public Dtos.JobView get(Long id) {
        return Dtos.JobView.from(jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job not found")));
    }

    public Job entity(Long id) {
        return jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job not found"));
    }

    @Transactional
    public Dtos.JobView create(User recruiter, Dtos.JobRequest request) {
        requireRecruiter(recruiter);
        Job job = new Job();
        job.setRecruiter(recruiter);
        apply(job, request);
        jobs.save(job);
        audit.record(recruiter, "JOB_CREATE", "Job", job.getId(), job.getTitle());
        return Dtos.JobView.from(job);
    }

    @Transactional
    public Dtos.JobView update(User recruiter, Long id, Dtos.JobRequest request) {
        Job job = entity(id);
        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You can only edit your own jobs");
        }
        apply(job, request);
        job.setUpdatedAt(Instant.now());
        audit.record(recruiter, "JOB_UPDATE", "Job", job.getId(), job.getTitle());
        return Dtos.JobView.from(job);
    }

    public List<Dtos.JobView> mine(User recruiter) {
        return jobs.findByRecruiterOrderByCreatedAtDesc(recruiter).stream().map(Dtos.JobView::from).toList();
    }

    private void apply(Job job, Dtos.JobRequest request) {
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setLocation(request.location());
        job.setEmploymentType(request.employmentType() == null ? EmploymentType.FULL_TIME : request.employmentType());
        job.setExperienceLevel(request.experienceLevel());
        job.setRequiredSkills(request.requiredSkills() == null ? List.of() : request.requiredSkills());
        job.setStatus(request.status() == null ? JobStatus.OPEN : request.status());
    }

    private void requireRecruiter(User user) {
        if (user.getRole() != Role.RECRUITER && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Recruiter role required");
        }
    }
}

package com.proofora.web;

import com.proofora.domain.EmploymentType;
import com.proofora.dto.Dtos;
import com.proofora.security.SecurityUtil;
import com.proofora.service.JobService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobs;

    public JobController(JobService jobs) {
        this.jobs = jobs;
    }

    @GetMapping
    public Dtos.PageResponse<Dtos.JobView> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EmploymentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return jobs.search(q, location, type, page, size);
    }

    @GetMapping("/{id}")
    public Dtos.JobView get(@PathVariable Long id) {
        return jobs.get(id);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public List<Dtos.JobView> mine() {
        return jobs.mine(SecurityUtil.currentUser());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public Dtos.JobView create(@Valid @RequestBody Dtos.JobRequest request) {
        return jobs.create(SecurityUtil.currentUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public Dtos.JobView update(@PathVariable Long id, @Valid @RequestBody Dtos.JobRequest request) {
        return jobs.update(SecurityUtil.currentUser(), id, request);
    }
}

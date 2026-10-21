package com.proofora.web;

import com.proofora.dto.Dtos;
import com.proofora.security.SecurityUtil;
import com.proofora.service.ApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService applications;

    public ApplicationController(ApplicationService applications) {
        this.applications = applications;
    }

    @PostMapping("/jobs/{jobId}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.ApplicationView apply(@PathVariable Long jobId, @RequestBody(required = false) Dtos.ApplyRequest request) {
        return applications.apply(SecurityUtil.currentUser(), jobId, request == null ? new Dtos.ApplyRequest("") : request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.ApplicationView> mine() {
        return applications.mine(SecurityUtil.currentUser());
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public List<Dtos.ApplicationView> forJob(@PathVariable Long jobId) {
        return applications.forJob(SecurityUtil.currentUser(), jobId);
    }

    @GetMapping("/{id}/timeline")
    public List<Dtos.TimelineEventView> timeline(@PathVariable Long id) {
        return applications.timeline(SecurityUtil.currentUser(), id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public Dtos.ApplicationView status(@PathVariable Long id, @RequestBody Dtos.StatusUpdateRequest request) {
        return applications.updateStatus(SecurityUtil.currentUser(), id, request);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.ApplicationView withdraw(@PathVariable Long id) {
        return applications.withdraw(SecurityUtil.currentUser(), id);
    }
}

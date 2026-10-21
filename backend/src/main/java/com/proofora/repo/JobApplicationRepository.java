package com.proofora.repo;

import com.proofora.domain.Job;
import com.proofora.domain.JobApplication;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByCandidateOrderByCreatedAtDesc(User candidate);
    List<JobApplication> findByJobOrderByCreatedAtDesc(Job job);
    Optional<JobApplication> findByJobAndCandidate(Job job, User candidate);
    boolean existsByJobAndCandidate(Job job, User candidate);
    long countByJobRecruiter(User recruiter);
}

package com.proofora.repo;

import com.proofora.domain.Job;
import com.proofora.domain.JobStatus;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findByRecruiterOrderByCreatedAtDesc(User recruiter);
    long countByStatus(JobStatus status);
}

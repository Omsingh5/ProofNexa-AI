package com.proofora.repo;

import com.proofora.domain.Job;
import com.proofora.domain.SkillGapReport;
import com.proofora.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillGapReportRepository extends JpaRepository<SkillGapReport, Long> {
    List<SkillGapReport> findByCandidateOrderByCreatedAtDesc(User candidate);
    List<SkillGapReport> findByCandidateAndJob(User candidate, Job job);
}

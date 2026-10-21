package com.proofora.repo;

import com.proofora.domain.GithubProject;
import com.proofora.domain.ProjectQualityReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectQualityReportRepository extends JpaRepository<ProjectQualityReport, Long> {
    List<ProjectQualityReport> findByProjectOrderByCreatedAtDesc(GithubProject project);
}

package com.proofora.service;

import com.proofora.ai.AiGateway;
import com.proofora.domain.AnalysisMode;
import com.proofora.domain.GithubProject;
import com.proofora.domain.ProjectQualityReport;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.repo.ProjectQualityReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QualityAnalyzerService {
    private final GithubAnalysisService github;
    private final ProjectQualityReportRepository reports;
    private final AiGateway ai;

    public QualityAnalyzerService(GithubAnalysisService github, ProjectQualityReportRepository reports, AiGateway ai) {
        this.github = github;
        this.reports = reports;
        this.ai = ai;
    }

    @Transactional
    public Dtos.QualityReportView analyze(User candidate, Long projectId, AnalysisMode mode) {
        GithubProject project = github.owned(candidate, projectId);
        String source = github.sampledSource(project);
        if (source.isBlank() && project.getFileTreeSummary() != null) {
            source = project.getFileTreeSummary();
        }
        if (source.isBlank()) {
            throw com.proofora.exception.ApiException.badRequest(
                    "No source files were available to analyze. Import a public repository first.");
        }
        AnalysisMode selected = mode == null ? AnalysisMode.VALIDATION : mode;
        ProjectQualityScorer.Result scored = ProjectQualityScorer.score(
                project.getOwner(), project.getName(), selected, source);
        String live = ai.complete(candidate,
                "Add at most four sentences of practical review. Cite only files and behaviors present in the sample. Do not invent metrics.",
                "Mode: " + selected + "\nRepo: " + project.getRepoUrl() + "\nNotes already written:\n"
                        + scored.architectureNotes() + "\n" + scored.recommendations() + "\nSample:\n"
                        + source.substring(0, Math.min(6000, source.length())));
        String recommendations = scored.recommendations();
        if (live != null && !live.isBlank()) {
            recommendations = recommendations + "\n\n" + live.trim();
        }
        ProjectQualityReport report = new ProjectQualityReport();
        report.setProject(project);
        report.setMode(selected);
        report.setArchitectureScore(scored.architecture());
        report.setMaintainabilityScore(scored.maintainability());
        report.setSecurityScore(scored.security());
        report.setTestingScore(scored.testing());
        report.setDataDesignScore(scored.dataDesign());
        report.setArchitectureNotes(scored.architectureNotes());
        report.setMaintainabilityNotes(scored.maintainabilityNotes());
        report.setSecurityFindings(scored.securityFindings());
        report.setTestingNotes(scored.testingNotes());
        report.setDataDesignNotes(scored.dataDesignNotes());
        report.setRecommendations(recommendations);
        report.setEvidenceSummary(scored.evidenceSummary());
        reports.save(report);
        return Dtos.QualityReportView.from(report);
    }

    public List<Dtos.QualityReportView> history(User candidate, Long projectId) {
        GithubProject project = github.owned(candidate, projectId);
        return reports.findByProjectOrderByCreatedAtDesc(project).stream().map(Dtos.QualityReportView::from).toList();
    }

}

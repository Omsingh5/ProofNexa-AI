package com.proofora.web;

import com.proofora.ai.ChatUpload;
import com.proofora.domain.AnalysisMode;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.security.SecurityUtil;
import com.proofora.service.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final ResumeAiService resumeAi;
    private final GithubAnalysisService github;
    private final QualityAnalyzerService quality;
    private final CareerGrowthService growth;
    private final AssistantService assistant;
    private final AnalyticsService analytics;
    private final NexaService nexa;

    public AiController(ResumeAiService resumeAi, GithubAnalysisService github, QualityAnalyzerService quality,
                        CareerGrowthService growth, AssistantService assistant, AnalyticsService analytics,
                        NexaService nexa) {
        this.resumeAi = resumeAi;
        this.github = github;
        this.quality = quality;
        this.growth = growth;
        this.assistant = assistant;
        this.analytics = analytics;
        this.nexa = nexa;
    }

    @PostMapping("/resume/analyze")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.SkillExtractionView analyzeResume() {
        return resumeAi.analyzeResume(SecurityUtil.currentUser());
    }

    @GetMapping("/resume")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.SkillExtractionView latestResume() {
        return resumeAi.latest(SecurityUtil.currentUser());
    }

    @PutMapping("/resume/skills")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.SkillExtractionView correct(@RequestBody Dtos.SkillCorrectionRequest request) {
        return resumeAi.correct(SecurityUtil.currentUser(), request.skills());
    }

    @PostMapping("/jobs/{jobId}/gap")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.SkillGapView gap(@PathVariable Long jobId) {
        return resumeAi.gap(SecurityUtil.currentUser(), jobId);
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.RecommendationView> recommendations() {
        return resumeAi.recommend(SecurityUtil.currentUser());
    }

    @GetMapping("/projects")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.GithubProjectView> projects() {
        return github.list(SecurityUtil.currentUser());
    }

    @PostMapping("/projects")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.GithubProjectView importRepo(@RequestBody Dtos.RepoSubmitRequest request) {
        return github.importRepo(SecurityUtil.currentUser(), request.repoUrl());
    }

    @PostMapping("/projects/{id}/reanalyze")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.GithubProjectView reanalyze(@PathVariable Long id) {
        return github.reanalyze(SecurityUtil.currentUser(), id);
    }

    @GetMapping("/projects/{id}/evidence")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.EvidenceView> evidence(@PathVariable Long id) {
        return github.evidenceFor(SecurityUtil.currentUser(), id);
    }

    @PostMapping("/projects/{id}/quality")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.QualityReportView quality(@PathVariable Long id, @RequestBody(required = false) Dtos.AnalyzeRequest request) {
        AnalysisMode mode = request == null || request.mode() == null ? AnalysisMode.VALIDATION : request.mode();
        return quality.analyze(SecurityUtil.currentUser(), id, mode);
    }

    @GetMapping("/projects/{id}/quality")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.QualityReportView> qualityHistory(@PathVariable Long id) {
        return quality.history(SecurityUtil.currentUser(), id);
    }

    @PostMapping("/roadmaps")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.RoadmapView roadmap(@RequestBody Map<String, Long> body) {
        return growth.generateRoadmap(SecurityUtil.currentUser(), body.get("jobId"), body.get("projectId"));
    }

    @GetMapping("/roadmaps")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.RoadmapView> roadmaps() {
        return growth.listRoadmaps(SecurityUtil.currentUser());
    }

    @PostMapping("/challenges")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.ChallengeView challenge(@RequestBody Map<String, String> body) {
        return growth.generateChallenge(SecurityUtil.currentUser(), body.get("skill"));
    }

    @GetMapping("/challenges")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<Dtos.ChallengeView> challenges() {
        return growth.listChallenges(SecurityUtil.currentUser());
    }

    @PostMapping("/challenges/{id}/submit")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Dtos.ChallengeView submit(@PathVariable Long id, @RequestBody Dtos.ChallengeSubmitRequest request) {
        return growth.submit(SecurityUtil.currentUser(), id, request.submission());
    }

    @GetMapping("/assistant")
    public List<Dtos.AssistantMessageView> assistantHistory() {
        return assistant.history(SecurityUtil.currentUser());
    }

    @PostMapping("/assistant")
    public Dtos.AssistantMessageView ask(@RequestBody Dtos.AssistantAskRequest request) {
        return assistant.ask(SecurityUtil.currentUser(), request.message());
    }

    @PostMapping(value = "/nexa", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Dtos.NexaReply nexa(@RequestBody Dtos.NexaRequest request) {
        return nexa.ask(SecurityUtil.currentUser(), request.message(), request.confirmAction());
    }

    @PostMapping(value = "/nexa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dtos.NexaReply nexaUpload(@RequestParam(required = false) String message,
                                     @RequestParam(required = false) String confirmAction,
                                     @RequestParam(required = false) List<MultipartFile> files) {
        return nexa.ask(SecurityUtil.currentUser(), message, confirmAction, readUploads(files));
    }

    private List<ChatUpload> readUploads(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > ChatUpload.MAX_FILES) {
            throw ApiException.badRequest("Attach up to 3 files.");
        }
        List<ChatUpload> uploads = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw ApiException.badRequest("One of the attachments is empty.");
            }
            if (file.getSize() > ChatUpload.MAX_BYTES) {
                throw ApiException.badRequest("Each attachment must be 4 MB or smaller.");
            }
            try {
                uploads.add(ChatUpload.parse(file.getOriginalFilename(), file.getBytes()));
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ex) {
                throw ApiException.badRequest("Could not read " + file.getOriginalFilename() + ".");
            }
        }
        return uploads;
    }

    @PostMapping("/jobs/{jobId}/interview")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public Dtos.InterviewPackView interview(@PathVariable Long jobId, @RequestParam(required = false) Long applicationId) {
        return assistant.interview(SecurityUtil.currentUser(), jobId, applicationId);
    }

    @PostMapping("/jd")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public Map<String, String> jd(@RequestBody Dtos.JdAssistRequest request) {
        return Map.of("markdown", assistant.writeJd(SecurityUtil.currentUser(), request.title(), request.notes()));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public Dtos.AnalyticsSummary analytics() {
        return analytics.summarize(SecurityUtil.currentUser());
    }
}

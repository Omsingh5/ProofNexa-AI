package com.proofora.service;

import com.proofora.ai.AiGateway;
import com.proofora.domain.*;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.JobRepository;
import com.proofora.repo.LearningRoadmapRepository;
import com.proofora.repo.TechnicalChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CareerGrowthService {
    private final LearningRoadmapRepository roadmaps;
    private final TechnicalChallengeRepository challenges;
    private final JobRepository jobs;
    private final ResumeAiService resumeAi;
    private final GithubAnalysisService github;
    private final AiGateway ai;

    public CareerGrowthService(LearningRoadmapRepository roadmaps, TechnicalChallengeRepository challenges,
                               JobRepository jobs, ResumeAiService resumeAi, GithubAnalysisService github,
                               AiGateway ai) {
        this.roadmaps = roadmaps;
        this.challenges = challenges;
        this.jobs = jobs;
        this.resumeAi = resumeAi;
        this.github = github;
        this.ai = ai;
    }

    public List<Dtos.RoadmapView> listRoadmaps(User candidate) {
        return roadmaps.findByCandidateOrderByCreatedAtDesc(candidate).stream()
                .map(r -> new Dtos.RoadmapView(r.getId(), r.getTitle(), r.getContentMarkdown(),
                        r.getTargetJob() == null ? null : r.getTargetJob().getId(), r.getCreatedAt()))
                .toList();
    }

    @Transactional
    public Dtos.RoadmapView generateRoadmap(User candidate, Long jobId, Long projectId) {
        Job job = jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job not found"));
        Dtos.SkillGapView gap = resumeAi.gap(candidate, jobId);
        List<String> unproven = projectId == null ? gap.missingSkills()
                : github.missingAgainstJob(candidate, projectId, job.getRequiredSkills());
        String fallback = """
                # 4-week proof plan for %s

                ## Week 1 — Close the loudest gaps
                %s

                ## Week 2 — Ship evidence
                Publish a small public repo that exercises the missing skills with tests.

                ## Week 3 — Depth
                Add architecture notes, a README, and one performance or security improvement.

                ## Week 4 — Interview artifacts
                Write 5 talking points that map files in your repo to this job's requirements.
                """.formatted(job.getTitle(),
                unproven.isEmpty() ? "- Keep sharpening already proven skills." : "- " + String.join("\n- ", unproven));
        String live = ai.complete(candidate,
                "Create a practical 4-week technical learning roadmap in markdown. No fluff.",
                "Job: " + job.getTitle() + "\nMissing/unproven: " + unproven + "\nGap notes: " + gap.explanation());
        LearningRoadmap roadmap = new LearningRoadmap();
        roadmap.setCandidate(candidate);
        roadmap.setTargetJob(job);
        roadmap.setTitle("Roadmap toward " + job.getTitle());
        roadmap.setContentMarkdown(live == null ? fallback : live);
        roadmaps.save(roadmap);
        return new Dtos.RoadmapView(roadmap.getId(), roadmap.getTitle(), roadmap.getContentMarkdown(),
                job.getId(), roadmap.getCreatedAt());
    }

    public List<Dtos.ChallengeView> listChallenges(User candidate) {
        return challenges.findByCandidateOrderByCreatedAtDesc(candidate).stream().map(Dtos.ChallengeView::from).toList();
    }

    @Transactional
    public Dtos.ChallengeView generateChallenge(User candidate, String skill) {
        String target = (skill == null || skill.isBlank()) ? "Spring Boot" : skill;
        String prompt = "Build a tiny " + target + " slice: one endpoint, one test, and a 5-line README explaining a tradeoff. Do not include secrets. Submit the design and code as text — Proofora never executes it.";
        String live = ai.complete(candidate,
                "Write one practical take-home challenge. Include constraints, acceptance criteria, and what NOT to do. Code will not be executed.",
                "Skill: " + target);
        TechnicalChallenge challenge = new TechnicalChallenge();
        challenge.setCandidate(candidate);
        challenge.setSkill(target);
        challenge.setTitle(target + " proof challenge");
        challenge.setPrompt(live == null ? prompt : live);
        challenges.save(challenge);
        return Dtos.ChallengeView.from(challenge);
    }

    @Transactional
    public Dtos.ChallengeView submit(User candidate, Long id, String submission) {
        TechnicalChallenge challenge = challenges.findById(id)
                .orElseThrow(() -> ApiException.notFound("Challenge not found"));
        if (!challenge.getCandidate().getId().equals(candidate.getId())) {
            throw ApiException.forbidden("Not your challenge");
        }
        challenge.setSubmission(submission);
        challenge.setStatus(ChallengeStatus.SUBMITTED);
        String feedback = "Reviewer notes (static): submission received. Look for tests, naming, and absence of secrets. Code was not executed.";
        String live = ai.complete(candidate,
                "Review a take-home as a staff engineer. Never ask to run the code. Give 5 bullets: strengths, gaps, next proof.",
                "Prompt: " + challenge.getPrompt() + "\nSubmission:\n" + submission);
        if (live != null) {
            feedback = live;
        }
        challenge.setFeedback(feedback);
        challenge.setStatus(ChallengeStatus.COMPLETED);
        challenge.setCompletedAt(Instant.now());
        challenges.save(challenge);
        return Dtos.ChallengeView.from(challenge);
    }
}

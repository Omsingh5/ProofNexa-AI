package com.proofora.service;

import com.proofora.ai.AiGateway;
import com.proofora.domain.AssistantMessage;
import com.proofora.domain.Job;
import com.proofora.domain.KnowledgeChunk;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.AssistantMessageRepository;
import com.proofora.repo.InterviewPackRepository;
import com.proofora.repo.JobRepository;
import com.proofora.repo.KnowledgeChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AssistantService {
    private final AssistantMessageRepository messages;
    private final KnowledgeChunkRepository chunks;
    private final JobRepository jobs;
    private final InterviewPackRepository packs;
    private final AiGateway ai;
    public AssistantService(AssistantMessageRepository messages, KnowledgeChunkRepository chunks,
                            JobRepository jobs, InterviewPackRepository packs, AiGateway ai) {
        this.messages = messages;
        this.chunks = chunks;
        this.jobs = jobs;
        this.packs = packs;
        this.ai = ai;
    }

    public List<Dtos.AssistantMessageView> history(User user) {
        return messages.findByUserOrderByCreatedAtAsc(user).stream()
                .map(m -> new Dtos.AssistantMessageView(m.getRole(), m.getContent(), m.getSources(), m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public Dtos.AssistantMessageView ask(User user, String question) {
        List<KnowledgeChunk> ranked = retrieve(question);
        String context = ranked.stream()
                .map(c -> "### " + c.getTitle() + "\n" + c.getBody())
                .collect(Collectors.joining("\n\n"));
        String sources = ranked.stream().map(KnowledgeChunk::getTitle).collect(Collectors.joining(", "));
        String fallback = ranked.isEmpty()
                ? "I only answer from authorized Proofora knowledge and your jobs. Try asking about applications, skill proof, or a listed job."
                : "Based on authorized notes:\n" + context.substring(0, Math.min(1200, context.length()));
        String live = ai.complete(user,
                "You are Proofora's career assistant. Use ONLY the provided authorized context. If missing, say you don't know. Cite source titles.",
                "Context:\n" + context + "\n\nQuestion:\n" + question);
        save(user, "user", question, null);
        AssistantMessage reply = save(user, "assistant", live == null ? fallback : live, sources);
        return new Dtos.AssistantMessageView(reply.getRole(), reply.getContent(), reply.getSources(), reply.getCreatedAt());
    }

    @Transactional
    public Dtos.InterviewPackView interview(User recruiter, Long jobId, Long applicationId) {
        Job job = jobs.findById(jobId).orElseThrow(() -> ApiException.notFound("Job not found"));
        String evidence = "";
        if (applicationId != null) {
            evidence = "Application id " + applicationId;
        }
        String md = """
                # Interview pack — %s

                1. Walk through a production bug you diagnosed. Where is the evidence in your repo?
                2. How would you model the core tables for this product?
                3. Required skills drill: %s
                4. Security: how do you store resumes and secrets?
                5. System design: scale job search with pagination and indexes.
                """.formatted(job.getTitle(), String.join(", ", job.getRequiredSkills()));
        String live = ai.complete(recruiter,
                "Write 8 interview questions mixing behavioral, depth, and evidence. Markdown only.",
                "Job: " + job.getTitle() + "\nDescription: " + job.getDescription() + "\n" + evidence);
        var pack = new com.proofora.domain.InterviewPack();
        pack.setJob(job);
        pack.setQuestionsMarkdown(live == null ? md : live);
        packs.save(pack);
        return new Dtos.InterviewPackView(pack.getId(), job.getId(), pack.getQuestionsMarkdown(), pack.getCreatedAt());
    }

    @Transactional
    public String writeJd(User recruiter, String title, String notes) {
        String fallback = """
                Role: %s

                About the work
                %s

                You will
                - Ship features with tests and clear pull requests
                - Collaborate with recruiters and engineers on skill-proof reviews

                Requirements
                - Evidence of the stack in public or private work samples
                """.formatted(title == null ? "Software Engineer" : title, notes);
        String live = ai.complete(recruiter,
                "Write a specific, inclusive job description. Avoid buzzword soup. Markdown.",
                "Title: " + title + "\nNotes: " + notes);
        return live == null ? fallback : live;
    }

    private List<KnowledgeChunk> retrieve(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        return chunks.findAll().stream()
                .sorted(Comparator.comparingInt((KnowledgeChunk c) -> score(q, c)).reversed())
                .limit(4)
                .filter(c -> score(q, c) > 0)
                .toList();
    }

    private int score(String q, KnowledgeChunk c) {
        int s = 0;
        String hay = (c.getTitle() + " " + c.getBody()).toLowerCase(Locale.ROOT);
        for (String token : q.split("\\W+")) {
            if (token.length() > 3 && hay.contains(token)) {
                s++;
            }
        }
        return s;
    }

    public void remember(User user, String role, String content, String sources) {
        save(user, role, content, sources);
    }

    private AssistantMessage save(User user, String role, String content, String sources) {
        AssistantMessage m = new AssistantMessage();
        m.setUser(user);
        m.setRole(role);
        m.setContent(content);
        m.setSources(sources);
        return messages.save(m);
    }
}

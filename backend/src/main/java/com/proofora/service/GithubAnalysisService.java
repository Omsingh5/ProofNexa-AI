package com.proofora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofora.ai.SkillTaxonomy;
import com.proofora.config.ProoforaProperties;
import com.proofora.domain.GithubProject;
import com.proofora.domain.SkillEvidence;
import com.proofora.domain.User;
import com.proofora.dto.Dtos;
import com.proofora.exception.ApiException;
import com.proofora.repo.GithubProjectRepository;
import com.proofora.repo.SkillEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GithubAnalysisService {
    private static final Pattern REPO = Pattern.compile("github\\.com/([^/]+)/([^/#?]+)");
    private static final int MAX_ARCHIVE_BYTES = 25_000_000;
    private static final int MAX_FILES = 40;
    private static final int MAX_FILE_BYTES = 80_000;
    private final GithubProjectRepository projects;
    private final SkillEvidenceRepository evidence;
    private final ProoforaProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public GithubAnalysisService(GithubProjectRepository projects, SkillEvidenceRepository evidence,
                                 ProoforaProperties properties, ObjectMapper mapper) {
        this.projects = projects;
        this.evidence = evidence;
        this.properties = properties;
        this.mapper = mapper;
    }

    public List<Dtos.GithubProjectView> list(User candidate) {
        return projects.findByCandidateOrderByIdDesc(candidate).stream().map(Dtos.GithubProjectView::from).toList();
    }

    public GithubProject owned(User candidate, Long id) {
        GithubProject project = projects.findById(id).orElseThrow(() -> ApiException.notFound("Project not found"));
        if (!project.getCandidate().getId().equals(candidate.getId()) && candidate.getRole() != com.proofora.domain.Role.ADMIN) {
            throw ApiException.forbidden("Not your project");
        }
        return project;
    }

    @Transactional
    public Dtos.GithubProjectView importRepo(User candidate, String repoUrl) {
        Matcher m = REPO.matcher(repoUrl);
        if (!m.find()) {
            throw ApiException.badRequest("Provide a GitHub URL like https://github.com/owner/repo");
        }
        String owner = m.group(1);
        String name = m.group(2).replace(".git", "");
        String canonical = "https://github.com/" + owner + "/" + name;
        GithubProject project = projects.findByCandidateOrderByIdDesc(candidate).stream()
                .filter(existing -> canonical.equalsIgnoreCase(existing.getRepoUrl()))
                .findFirst()
                .orElseGet(GithubProject::new);
        project.setCandidate(candidate);
        project.setRepoUrl(canonical);
        project.setOwner(owner);
        project.setName(name);
        JsonNode repo = getJson("https://api.github.com/repos/" + owner + "/" + name);
        if (repo != null) {
            project.setDefaultBranch(repo.path("default_branch").asText("main"));
            project.setDescription(repo.path("description").asText(""));
        } else if (project.getDefaultBranch() == null) {
            project.setDefaultBranch("main");
            project.setDescription("Public repository. GitHub's metadata API was unavailable, so files were read from the public archive.");
        }
        projects.save(project);
        analyze(project);
        return Dtos.GithubProjectView.from(project);
    }

    @Transactional
    public Dtos.GithubProjectView reanalyze(User candidate, Long id) {
        GithubProject project = owned(candidate, id);
        analyze(project);
        return Dtos.GithubProjectView.from(project);
    }

    public List<Dtos.EvidenceView> evidenceFor(User candidate, Long projectId) {
        GithubProject project = owned(candidate, projectId);
        return evidence.findByProject(project).stream()
                .map(e -> new Dtos.EvidenceView(e.getSkill(), e.getFilePath(), e.getSnippet(), e.getConfidence()))
                .toList();
    }

    public List<String> missingAgainstJob(User candidate, Long projectId, List<String> required) {
        GithubProject project = owned(candidate, projectId);
        Set<String> proven = new LinkedHashSet<>(project.getDetectedTechnologies());
        evidence.findByProject(project).forEach(e -> proven.add(e.getSkill()));
        return required.stream()
                .filter(r -> proven.stream().noneMatch(p -> p.equalsIgnoreCase(r)))
                .toList();
    }

    public String sampledSource(GithubProject project) {
        return fetchTreeAndFiles(project, false);
    }

    private void analyze(GithubProject project) {
        evidence.deleteByProject(project);
        String tree = fetchTreeAndFiles(project, true);
        project.setFileTreeSummary(tree.substring(0, Math.min(tree.length(), 15000)));
        Set<String> tech = SkillTaxonomy.detect(tree);
        JsonNode langs = getJson("https://api.github.com/repos/" + project.getOwner() + "/" + project.getName() + "/languages");
        if (langs != null) {
            langs.fieldNames().forEachRemaining(tech::add);
        }
        project.setDetectedTechnologies(new ArrayList<>(tech));
        project.setLastAnalyzedAt(Instant.now());
        projects.save(project);
    }

    private String fetchTreeAndFiles(GithubProject project, boolean storeEvidence) {
        byte[] archive = downloadArchive(project);
        List<HeldFile> held = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String path = relativePath(entry.getName());
                if (path.isBlank() || path.contains("..") || !isSource(path)) {
                    continue;
                }
                byte[] bytes = zip.readNBytes(MAX_FILE_BYTES + 1);
                if (bytes.length == 0 || bytes.length > MAX_FILE_BYTES || !isText(bytes)) {
                    continue;
                }
                String decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                keepBest(held, new HeldFile(path, decoded, sourcePriority(path)));
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.badRequest("Could not read the public repository archive.");
        }
        StringBuilder collected = new StringBuilder();
        int files = 0;
        for (HeldFile file : held) {
            collected.append(file.path).append('\n');
            collected.append("\n--- ").append(file.path).append(" ---\n")
                    .append(file.text, 0, Math.min(file.text.length(), 2500));
            files++;
            if (storeEvidence) {
                storeEvidence(project, file.path, file.text);
            }
        }
        if (files == 0) {
            throw ApiException.badRequest("No supported source files were found in " + project.getOwner() + "/" + project.getName() + ".");
        }
        return collected.toString();
    }

    private byte[] downloadArchive(GithubProject project) {
        String branch = project.getDefaultBranch() == null || project.getDefaultBranch().isBlank()
                ? "main" : project.getDefaultBranch();
        byte[] archive = downloadZip(project.getOwner(), project.getName(), branch);
        if (archive == null && !"master".equals(branch)) {
            archive = downloadZip(project.getOwner(), project.getName(), "master");
            if (archive != null) {
                project.setDefaultBranch("master");
            }
        }
        if (archive == null && !"main".equals(branch)) {
            archive = downloadZip(project.getOwner(), project.getName(), "main");
            if (archive != null) {
                project.setDefaultBranch("main");
            }
        }
        if (archive == null) {
            throw ApiException.badRequest("Could not download a public archive for "
                    + project.getOwner() + "/" + project.getName()
                    + ". The repository may be private, missing, or too large.");
        }
        return archive;
    }

    private byte[] downloadZip(String owner, String name, String branch) {
        String url = "https://codeload.github.com/" + owner + "/" + name + "/zip/refs/heads/" + branch;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .header("User-Agent", "ProofNexa-AI")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return null;
            }
            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > MAX_ARCHIVE_BYTES) {
                return null;
            }
            return body;
        } catch (Exception ex) {
            return null;
        }
    }

    private String relativePath(String entryName) {
        int slash = entryName.indexOf('/');
        return slash >= 0 ? entryName.substring(slash + 1) : entryName;
    }

    private void keepBest(List<HeldFile> held, HeldFile candidate) {
        if (held.size() < MAX_FILES) {
            held.add(candidate);
            return;
        }
        int worst = 0;
        for (int i = 1; i < held.size(); i++) {
            if (held.get(i).priority < held.get(worst).priority) {
                worst = i;
            }
        }
        if (candidate.priority > held.get(worst).priority) {
            held.set(worst, candidate);
        }
    }

    private int sourcePriority(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.startsWith(".github") || lower.contains("/.github/") || lower.contains("devcontainer") || lower.contains("gitpod")) {
            return 0;
        }
        if (lower.endsWith("package-lock.json") || lower.endsWith("changelog.md")) {
            return 1;
        }
        if (lower.contains("/test/") || lower.contains(".spec.") || lower.contains(".test.") || lower.endsWith("test.java")) {
            return 6;
        }
        if (lower.endsWith("readme.md")) {
            return 5;
        }
        if (lower.endsWith("pom.xml") || lower.endsWith("package.json")) {
            return 8;
        }
        if (lower.endsWith(".java") || lower.endsWith(".jsx") || lower.endsWith(".tsx") || lower.endsWith(".vue")) {
            return 9;
        }
        if (lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".py") || lower.endsWith(".html") || lower.endsWith(".css")) {
            return 7;
        }
        return 3;
    }

    private static final class HeldFile {
        private final String path;
        private final String text;
        private final int priority;

        private HeldFile(String path, String text, int priority) {
            this.path = path;
            this.text = text;
            this.priority = priority;
        }
    }

    private boolean isText(byte[] bytes) {
        int check = Math.min(bytes.length, 800);
        for (int i = 0; i < check; i++) {
            if (bytes[i] == 0) {
                return false;
            }
        }
        return true;
    }

    private void storeEvidence(GithubProject project, String path, String content) {
        Set<String> skills = SkillTaxonomy.detect(content + " " + path);
        for (String skill : skills) {
            SkillEvidence row = new SkillEvidence();
            row.setProject(project);
            row.setSkill(skill);
            row.setFilePath(path);
            row.setSnippet(firstUsefulLine(content));
            row.setConfidence(70);
            evidence.save(row);
        }
    }

    private String firstUsefulLine(String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.length() < 12 || trimmed.startsWith("*") || trimmed.startsWith("//")) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.contains("copyright") || lower.startsWith("package ") || lower.startsWith("import ")) {
                continue;
            }
            return trimmed.length() > 180 ? trimmed.substring(0, 180) : trimmed;
        }
        return content.substring(0, Math.min(180, content.length())).replace('\n', ' ');
    }

    private boolean isSource(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return (lower.matches(".*\\.(java|js|jsx|ts|tsx|py|sql|yml|yaml|xml|gradle|css|html|properties|md)$")
                || lower.endsWith("pom.xml")
                || lower.endsWith("package.json")
                || lower.endsWith("dockerfile"))
                && !lower.contains("node_modules")
                && !lower.contains("/target/");
    }

    private JsonNode getJson(String url) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "ProofNexa-AI")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28");
            if (properties.getGithub().getToken() != null && !properties.getGithub().getToken().isBlank()) {
                builder.header("Authorization", "Bearer " + properties.getGithub().getToken());
            }
            HttpResponse<String> response = http.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }
            return mapper.readTree(response.body());
        } catch (Exception e) {
            return null;
        }
    }
}

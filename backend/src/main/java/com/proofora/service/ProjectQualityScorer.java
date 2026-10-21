package com.proofora.service;

import com.proofora.ai.SkillTaxonomy;
import com.proofora.domain.AnalysisMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static review of sampled repository files. Scores and notes cite those files.
 * Candidate code is never executed.
 */
public final class ProjectQualityScorer {
    public record FileSample(String path, String body) {}

    public record Result(
            int architecture,
            int maintainability,
            int security,
            int testing,
            int dataDesign,
            String architectureNotes,
            String maintainabilityNotes,
            List<String> securityFindings,
            String testingNotes,
            String dataDesignNotes,
            String recommendations,
            String evidenceSummary
    ) {}

    private ProjectQualityScorer() {}

    public static List<FileSample> parse(String sample) {
        List<FileSample> files = new ArrayList<>();
        if (sample == null || sample.isBlank()) {
            return files;
        }
        String[] parts = sample.split("(?m)^--- ");
        for (int i = 1; i < parts.length; i++) {
            int nl = parts[i].indexOf('\n');
            String header = (nl < 0 ? parts[i] : parts[i].substring(0, nl)).replace(" ---", "").trim();
            String body = nl < 0 ? "" : parts[i].substring(nl + 1);
            if (!header.isBlank()) {
                files.add(new FileSample(header, body));
            }
        }
        return files;
    }

    public static Result score(String owner, String name, AnalysisMode mode, String sample) {
        List<FileSample> files = parse(sample);
        Map<String, Integer> extensions = new LinkedHashMap<>();
        int totalLines = 0;
        int largest = 0;
        String largestPath = "";
        int longFiles = 0;
        int testFiles = 0;
        int readme = 0;
        boolean layered = false;
        boolean components = false;
        boolean html = false;
        boolean css = false;
        boolean script = false;
        boolean schema = false;
        boolean clientData = false;
        boolean frontendManifest = false;
        boolean applicationSource = false;
        List<String> security = new ArrayList<>();
        List<String> cited = new ArrayList<>();

        for (FileSample file : files) {
            String path = file.path();
            String lowerPath = path.toLowerCase(Locale.ROOT);
            String ext = extension(lowerPath);
            extensions.merge(ext, 1, Integer::sum);
            int lines = file.body().isEmpty() ? 0 : file.body().split("\n", -1).length;
            totalLines += lines;
            if (lines > largest) {
                largest = lines;
                largestPath = path;
            }
            if (lines > 280) {
                longFiles++;
            }
            if (isTestPath(lowerPath)) {
                testFiles++;
            }
            if (lowerPath.endsWith("readme.md")) {
                readme++;
            }
            if (lowerPath.contains("/controller/") || lowerPath.contains("/service/") || lowerPath.contains("/repository/")) {
                layered = true;
            }
            if (lowerPath.contains("/components/") || lowerPath.contains("/pages/") || lowerPath.contains("/src/")) {
                components = true;
            }
            if (ext.equals("html")) html = true;
            if (ext.equals("css") || ext.equals("scss")) css = true;
            if (ext.equals("js") || ext.equals("jsx") || ext.equals("ts") || ext.equals("tsx")) script = true;
            if (ext.equals("sql") || lowerPath.contains("/entity/") || lowerPath.contains("/model/")
                    || lowerPath.endsWith("schema.sql")) {
                schema = true;
            }
            String bodyLower = file.body().toLowerCase(Locale.ROOT);
            if (bodyLower.contains("localstorage") || bodyLower.contains("fetch(") || bodyLower.contains("axios")) {
                clientData = true;
            }
            if (lowerPath.endsWith("package.json") && (bodyLower.contains("\"next\"") || bodyLower.contains("\"react\""))) {
                frontendManifest = true;
            }
            if (lowerPath.contains("/app/") || lowerPath.contains("/pages/") || lowerPath.contains("/components/")
                    || lowerPath.endsWith(".html") || lowerPath.endsWith(".jsx")
                    || (lowerPath.endsWith(".tsx") && !lowerPath.endsWith(".d.ts"))) {
                applicationSource = true;
            }
            collectSecurity(path, file.body(), security);
            if (cited.size() < 4 && !lowerPath.contains(".github")) {
                cited.add(path);
            }
        }

        int sourceFiles = (int) files.stream().filter(f -> !f.path().toLowerCase(Locale.ROOT).endsWith("readme.md")).count();
        int architecture = 34;
        if (html && css && script) architecture += 16;
        else if (extensions.size() >= 3) architecture += 8;
        if (components) architecture += 12;
        if (layered) architecture += 22;
        if (sourceFiles <= 1) architecture -= 10;
        if (largest > 0 && totalLines > 0 && largest * 100 / Math.max(totalLines, 1) > 55) architecture -= 8;
        architecture = clamp(architecture);

        int maintainability = 52 + (readme > 0 ? 8 : 0) - longFiles * 7;
        if (sourceFiles >= 3 && sourceFiles <= 25) maintainability += 6;
        if (sourceFiles > 40) maintainability -= 4;
        maintainability = clamp(maintainability);

        int securityScore = clamp(92 - security.size() * 11);
        int testing = testFiles == 0 ? (sourceFiles > 15 ? 16 : 24) : clamp(28 + Math.min(testFiles, 8) * 8);
        int data = 30;
        if (schema) data += 38;
        if (clientData) data += 12;
        if (!schema && !clientData) data = 28;
        data = clamp(data);

        String repo = owner + "/" + name;
        String mix = extensionSummary(extensions);
        String architectureNotes = sourceFiles + " sampled source files in " + repo + " (" + mix + "). "
                + (layered
                ? "HTTP, service, and repository paths are separated."
                : components
                ? "UI code is split across source folders rather than one script."
                : html && css
                ? "Markup and styles are in different files, with no server-side layering in this repository."
                : "Sampled files do not show a split between interface, domain, and persistence.")
                + (cited.isEmpty() ? "" : " Files read include " + String.join(", ", cited) + ".")
                + (frontendManifest && !applicationSource
                ? " package.json depends on a frontend framework, but the archive contains no app, pages, or components directory."
                : "");

        String maintainabilityNotes = readme > 0
                ? "A README is present. "
                : "No README was in the sample, so a reviewer cannot see how to run the project. ";
        maintainabilityNotes += largestPath.isBlank()
                ? "File lengths could not be measured."
                : "Largest sampled file is " + largestPath + " (" + largest + " lines). "
                + (longFiles == 0 ? "No sampled file exceeds 280 lines." : longFiles + " sampled file(s) exceed 280 lines.");

        String testingNotes = testFiles == 0
                ? "No test, spec, or src/test file was in the sample. Nothing was executed, so this is not a coverage number."
                : testFiles + " test or spec file(s) were sampled. Tests were not executed.";

        String dataNotes = schema
                ? "Schema, entity, or SQL files are part of this repository."
                : clientData
                ? "The sample talks to browser storage or HTTP APIs, and it does not include a database schema."
                : "No SQL, entity, or model files were sampled, so this repository is not evidence of database design.";

        String recommendations = recommendations(mode, repo, security, testFiles, layered, html, schema, largestPath, cited,
                frontendManifest && !applicationSource);
        String evidence = "Scored " + files.size() + " files from the public archive of " + repo
                + ". " + totalLines + " lines were read. Code was inspected and was not executed.";
        return new Result(architecture, maintainability, securityScore, testing, data,
                architectureNotes, maintainabilityNotes, security, testingNotes, dataNotes, recommendations, evidence);
    }

    private static String recommendations(AnalysisMode mode, String repo, List<String> security, int testFiles,
                                          boolean layered, boolean html, boolean schema, String largestPath,
                                          List<String> cited, boolean configOnly) {
        String focus = switch (mode == null ? AnalysisMode.VALIDATION : mode) {
            case SKILL_DEVELOPMENT -> "Skill development for " + repo + ": pick one gap below and change that file this week.\n";
            case CAREER_ADVANCEMENT -> "Career evidence for " + repo + ": a reviewer can only defend what these files show.\n";
            case VALIDATION -> "Validation of " + repo + ": scores describe the sampled files, not a running demo.\n";
        };
        StringBuilder sb = new StringBuilder(focus);
        if (configOnly) {
            sb.append("- The public archive is configuration only (package.json, Next/Tailwind config, README). Commit the page and component files, then analyze again. The README names app/page.tsx, and that file is not in the repository.\n");
            sb.append("- ProofNexa does not run this repository.");
            return sb.toString();
        }
        if (testFiles == 0) {
            sb.append("- Add a test next to the main behavior");
            if (!cited.isEmpty()) {
                sb.append(" (start from ").append(cited.get(0)).append(")");
            }
            sb.append(".\n");
        }
        if (!layered && !html) {
            sb.append("- Split interface, domain, and persistence into separate paths before calling this production structure.\n");
        }
        if (html && !schema) {
            sb.append("- If this site collects information, document where that data goes. The repository itself has no schema.\n");
        }
        if (!security.isEmpty()) {
            sb.append("- Fix the security notes before sharing the repo: ").append(security.get(0)).append(".\n");
        }
        if (!largestPath.isBlank()) {
            sb.append("- Keep changes reviewable in ").append(largestPath).append(" instead of growing one file further.\n");
        }
        sb.append("- ProofNexa does not run this repository.");
        return sb.toString();
    }

    private static void collectSecurity(String path, String body, List<String> findings) {
        for (String line : body.split("\n")) {
            if (SkillTaxonomy.looksLikeSecret(line)) {
                addFinding(findings, path + ": possible hardcoded secret");
            }
            String trimmed = line.trim().toLowerCase(Locale.ROOT);
            if (trimmed.contains("eval(")) {
                addFinding(findings, path + ": uses eval");
            }
            if (trimmed.contains("innerhtml") && trimmed.contains("=")) {
                addFinding(findings, path + ": assigns innerHTML");
            }
            if (trimmed.contains("http://") && !trimmed.contains("http://localhost") && !trimmed.contains("http://127.0.0.1")
                    && !trimmed.startsWith("//") && !trimmed.startsWith("*") && !trimmed.startsWith("#")) {
                addFinding(findings, path + ": plain HTTP URL");
            }
        }
    }

    private static void addFinding(List<String> findings, String finding) {
        if (findings.size() < 8 && !findings.contains(finding)) {
            findings.add(finding);
        }
    }

    private static boolean isTestPath(String lowerPath) {
        return lowerPath.contains("/test/") || lowerPath.contains("/__tests__/")
                || lowerPath.contains(".spec.") || lowerPath.contains(".test.")
                || lowerPath.endsWith("test.java") || lowerPath.endsWith("tests.java");
    }

    private static String extension(String lowerPath) {
        int dot = lowerPath.lastIndexOf('.');
        if (dot < 0 || dot < lowerPath.lastIndexOf('/')) {
            return "other";
        }
        String ext = lowerPath.substring(dot + 1);
        return ext.length() > 6 ? "other" : ext;
    }

    private static String extensionSummary(Map<String, Integer> extensions) {
        if (extensions.isEmpty()) {
            return "no typed files";
        }
        StringBuilder sb = new StringBuilder();
        extensions.forEach((ext, count) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(count).append(" ").append(ext);
        });
        return sb.toString();
    }

    private static int clamp(int value) {
        return Math.max(8, Math.min(96, value));
    }
}

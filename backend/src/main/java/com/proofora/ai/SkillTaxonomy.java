package com.proofora.ai;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class SkillTaxonomy {
    public static final List<String> SKILLS = List.of(
            "Java", "Spring Boot", "Hibernate", "JPA", "React", "JavaScript", "TypeScript",
            "HTML5", "CSS3", "MySQL", "PostgreSQL", "REST", "Git", "GitHub", "Docker",
            "Kubernetes", "AWS", "Python", "Node.js", "Redis", "Kafka", "JUnit", "Mockito",
            "Security", "JWT", "Maven", "Gradle", "CI/CD", "SQL", "MongoDB", "Bootstrap", "Express"
    );

    private static final Map<String, Pattern> ALIASES = aliases();

    private SkillTaxonomy() {}

    public static Set<String> detect(String text) {
        String hay = text == null ? "" : text;
        Set<String> found = new LinkedHashSet<>();
        for (Map.Entry<String, Pattern> entry : ALIASES.entrySet()) {
            if (entry.getValue().matcher(hay).find()) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    private static Map<String, Pattern> aliases() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("Java", "\\bjava\\b");
        raw.put("Spring Boot", "spring\\s*boot");
        raw.put("Hibernate", "\\bhibernate\\b");
        raw.put("JPA", "\\bjpa\\b");
        raw.put("React", "\\breact(?:\\.js)?\\b");
        raw.put("JavaScript", "\\bjavascript\\b|\\bjs\\b");
        raw.put("TypeScript", "\\btypescript\\b");
        raw.put("HTML5", "\\bhtml5?\\b");
        raw.put("CSS3", "\\bcss3?\\b");
        raw.put("Bootstrap", "\\bbootstrap\\b");
        raw.put("MySQL", "\\bmysql\\b");
        raw.put("PostgreSQL", "\\bpostgres(?:ql)?\\b");
        raw.put("REST", "\\brestful\\b|\\brest\\s+apis?\\b");
        raw.put("GitHub", "\\bgithub\\b");
        raw.put("Git", "\\bgit\\b");
        raw.put("Docker", "\\bdocker\\b");
        raw.put("Kubernetes", "\\bkubernetes\\b|\\bk8s\\b");
        raw.put("AWS", "\\baws\\b");
        raw.put("Python", "\\bpython\\b");
        raw.put("Node.js", "\\bnode\\.?js\\b|\\bnodejs\\b");
        raw.put("Express", "\\bexpress(?:\\.js)?\\b");
        raw.put("Redis", "\\bredis\\b");
        raw.put("Kafka", "\\bkafka\\b");
        raw.put("JUnit", "\\bjunit\\b");
        raw.put("Mockito", "\\bmockito\\b");
        raw.put("JWT", "\\bjwt\\b");
        raw.put("Maven", "\\bmaven\\b");
        raw.put("Gradle", "\\bgradle\\b");
        raw.put("CI/CD", "\\bci/?cd\\b");
        raw.put("SQL", "\\bsql\\b");
        raw.put("MongoDB", "\\bmongodb\\b|\\bmongo\\b");
        Map<String, Pattern> compiled = new LinkedHashMap<>();
        raw.forEach((skill, regex) -> compiled.put(skill, Pattern.compile(regex, Pattern.CASE_INSENSITIVE)));
        return compiled;
    }

    public static boolean looksLikeSecret(String line) {
        return Pattern.compile("(?i)(api[_-]?key|secret|password)\\s*[=:]\\s*['\"][^'\"]{8,}").matcher(line).find();
    }
}

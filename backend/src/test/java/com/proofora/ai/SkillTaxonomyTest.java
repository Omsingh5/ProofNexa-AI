package com.proofora.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillTaxonomyTest {
    @Test
    void detectsStackFromResumeText() {
        var skills = SkillTaxonomy.detect("Built APIs with Java, Spring Boot, Hibernate and React.");
        assertThat(skills).contains("Java", "Spring Boot", "Hibernate", "React");
        assertThat(SkillTaxonomy.detect("JavaScript, HTML, CSS, and Node.js")).contains("JavaScript", "HTML5", "CSS3", "Node.js").doesNotContain("Java");
    }

    @Test
    void flagsHardcodedSecrets() {
        assertThat(SkillTaxonomy.looksLikeSecret("api_key = \"sk-live-12345678\"")).isTrue();
        assertThat(SkillTaxonomy.looksLikeSecret("const title = \"Engineer\"")).isFalse();
    }
}

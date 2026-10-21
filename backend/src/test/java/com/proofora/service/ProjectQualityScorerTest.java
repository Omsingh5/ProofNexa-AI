package com.proofora.service;

import com.proofora.domain.AnalysisMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectQualityScorerTest {
    @Test
    void scoresAStaticSiteDifferentlyFromALayeredService() {
        String site = """
                --- index.html ---
                <form action="/contact">
                --- styles.css ---
                body { margin: 0; }
                --- booking.js ---
                fetch("/api/book")
                """;
        String service = """
                --- src/main/java/app/controller/VisitController.java ---
                class VisitController {}
                --- src/main/java/app/service/VisitService.java ---
                class VisitService {}
                --- src/main/java/app/repository/VisitRepository.java ---
                interface VisitRepository {}
                --- src/test/java/app/VisitControllerTest.java ---
                class VisitControllerTest {}
                --- README.md ---
                Run the clinic API
                --- src/main/resources/schema.sql ---
                create table visits(id int);
                """;

        var siteScore = ProjectQualityScorer.score("Omsingh5", "TherapistWebsite", AnalysisMode.SKILL_DEVELOPMENT, site);
        var serviceScore = ProjectQualityScorer.score("spring-projects", "spring-petclinic", AnalysisMode.VALIDATION, service);

        assertThat(siteScore.architecture()).isLessThan(serviceScore.architecture());
        assertThat(siteScore.testing()).isLessThan(serviceScore.testing());
        assertThat(siteScore.architectureNotes()).contains("index.html");
        assertThat(siteScore.recommendations()).contains("TherapistWebsite");
        assertThat(serviceScore.testingNotes()).contains("test");
        assertThat(serviceScore.dataDesign()).isGreaterThan(siteScore.dataDesign());
        assertThat(siteScore.architecture()).isNotEqualTo(75);
        assertThat(siteScore.maintainability()).isNotEqualTo(70);
    }
}
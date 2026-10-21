package com.proofora.ai;

import com.proofora.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NexaIntentsTest {
    @Test
    void routesCandidateQuestionsToExistingWorkflows() {
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Analyze my technical skills")).isEqualTo("resume");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Find matching jobs")).isEqualTo("jobs");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Explain my project quality report")).isEqualTo("github");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Prepare me for an interview")).isEqualTo("interview");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "What is my application status?")).isEqualTo("applications");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Show my skill gaps")).isEqualTo("compare");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Show my roadmap")).isEqualTo("roadmap");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Help with my technical challenge")).isEqualTo("challenge");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "Submit my application")).isEqualTo("refuse-apply");
    }

    @Test
    void routesRecruiterQuestionsAndRefusesHiringDecisions() {
        assertThat(NexaIntents.feature(Role.RECRUITER, "Draft a job description for a Java role")).isEqualTo("jd");
        assertThat(NexaIntents.feature(Role.RECRUITER, "Summarize applicants")).isEqualTo("applicants");
        assertThat(NexaIntents.feature(Role.RECRUITER, "Plan an interview")).isEqualTo("interview-plan");
        assertThat(NexaIntents.feature(Role.RECRUITER, "Show application pipeline")).isEqualTo("pipeline");
        assertThat(NexaIntents.feature(Role.RECRUITER, "Explain recruitment analytics")).isEqualTo("analytics");
        assertThat(NexaIntents.feature(Role.RECRUITER, "Summarize technical evidence we have")).isEqualTo("evidence");
        assertThat(NexaIntents.feature(Role.RECRUITER, "Reject this applicant")).isEqualTo("refuse-decision");
        assertThat(NexaIntents.feature(Role.RECRUITER, "hi")).isEqualTo("greet");
        assertThat(NexaIntents.feature(Role.RECRUITER, "software engineer")).isEqualTo("jd");
        assertThat(NexaIntents.feature(Role.RECRUITER, "do it", "greet")).isEqualTo("jd");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "hello")).isEqualTo("greet");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "java developer")).isEqualTo("jobs");
        assertThat(NexaIntents.feature(Role.RECRUITER, "tell me which job fits best more me")).isEqualTo("focus");
        assertThat(NexaIntents.feature(Role.RECRUITER, "how can you help me ?")).isEqualTo("capabilities");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "what is an ATS")).isEqualTo("help");
        assertThat(NexaIntents.feature(Role.CANDIDATE, "which job fits me best")).isEqualTo("jobs");
        assertThat(NexaIntents.feature(Role.RECRUITER, "can you help with job")).isEqualTo("capabilities");
        assertThat(NexaIntents.feature(Role.RECRUITER, "can you check my resume and suggest me jobs")).isEqualTo("own-resume");
    }
}

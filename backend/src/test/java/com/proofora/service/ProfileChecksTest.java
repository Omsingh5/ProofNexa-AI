package com.proofora.service;

import com.proofora.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileChecksTest {
    @Test
    void acceptsWebLinksAndRejectsBareDomains() {
        assertThat(ProfileChecks.optionalUrl("https://example.com/in/ada", "LinkedIn")).contains("example.com");
        assertThatThrownBy(() -> ProfileChecks.optionalUrl("linkedin.com/in/ada", "LinkedIn"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void scoresFilledFields() {
        assertThat(ProfileChecks.completion(ProfileChecks.filled(true, false, true), 3)).isEqualTo(67);
    }
}

package com.proofora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofora.domain.Role;
import com.proofora.dto.Dtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "proofora.auth.login-max-attempts=2")
@AutoConfigureMockMvc
class LoginRateLimitTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void blocksRepeatedFailedLogins() throws Exception {
        var register = new Dtos.RegisterRequest("limited@example.com", "Password123!", "Limited User", Role.CANDIDATE, null);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        var bad = new Dtos.LoginRequest("limited@example.com", "WrongPassword1");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bad)))
                .andExpect(status().isTooManyRequests());
    }
}

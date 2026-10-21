package com.proofora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proofora.domain.Role;
import com.proofora.dto.Dtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityFlowTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @Test
    void rejectsWeakPasswords() throws Exception {
        var register = new Dtos.RegisterRequest("weak@example.com", "password", "Weak User", Role.CANDIDATE, null);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(register)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordThenRejectsReuse() throws Exception {
        register("reset@example.com");
        MvcResult forgot = mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.ForgotPasswordRequest("reset@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").isNotEmpty())
                .andReturn();
        String token = mapper.readTree(forgot.getResponse().getContentAsString()).get("resetToken").asText();

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.ResetPasswordRequest(token, "NewPassword1"))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.LoginRequest("reset@example.com", "NewPassword1"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.ResetPasswordRequest(token, "NewPassword2"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEmailDoesNotRevealAnAccount() throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.ForgotPasswordRequest("missing@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").doesNotExist())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void deactivatedAccountCannotAuthenticate() throws Exception {
        String access = register("gone@example.com");
        mvc.perform(post("/api/auth/deactivate")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.DeactivateRequest("Password123!"))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new Dtos.LoginRequest("gone@example.com", "Password123!"))))
                .andExpect(status().isForbidden());
    }

    private String register(String email) throws Exception {
        var register = new Dtos.RegisterRequest(email, "Password123!", "Test User", Role.CANDIDATE, null);
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}

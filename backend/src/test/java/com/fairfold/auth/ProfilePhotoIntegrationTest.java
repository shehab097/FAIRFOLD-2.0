package com.fairfold.auth;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfilePhotoIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Test
    void candidate_can_upload_view_and_remove_profile_photo() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG);

        MvcResult result = mockMvc.perform(multipart("/api/account/photo")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isNotEmpty())
                .andReturn();

        String avatarUrl = objectMapper.readTree(result.getResponse().getContentAsString()).get("avatarUrl").asText();
        assertTrue(avatarUrl.startsWith("/api/users/photo/"));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(avatarUrl));

        mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(delete("/api/account/photo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isEmpty());
    }

    @Test
    void employer_hr_can_upload_profile_photo() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG);

        mockMvc.perform(multipart("/api/account/photo")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(employer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYER_HR"))
                .andExpect(jsonPath("$.avatarUrl").isNotEmpty());
    }

    @Test
    void invalid_profile_photo_returns_400() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        MockMultipartFile file = new MockMultipartFile("file", "photo.txt", "text/plain", "not an image".getBytes());

        mockMvc.perform(multipart("/api/account/photo")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

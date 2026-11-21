package com.fairfold.auth;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfilePhotoIntegrationTest extends AbstractIntegrationTest {

    @Test
    void candidate_photo_is_converted_to_480_square_webp_and_can_be_removed() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", png(200, 100));

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

        MvcResult photoResult = mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.parseMediaType("image/webp")))
                .andReturn();

        byte[] webp = photoResult.getResponse().getContentAsByteArray();
        assertTrue(webp.length > 12);
        assertEquals('R', webp[0]);
        assertEquals('I', webp[1]);
        assertEquals('F', webp[2]);
        assertEquals('F', webp[3]);
        assertEquals('W', webp[8]);
        assertEquals('E', webp[9]);
        assertEquals('B', webp[10]);
        assertEquals('P', webp[11]);

        BufferedImage converted = ImageIO.read(new ByteArrayInputStream(webp));
        assertNotNull(converted);
        assertEquals(480, converted.getWidth());
        assertEquals(480, converted.getHeight());

        mockMvc.perform(delete("/api/account/photo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isEmpty());
    }

    @Test
    void employer_hr_can_upload_profile_photo() throws Exception {
        TestEmployer employer = registerEmployer("hr@acme.com");
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", png(100, 200));

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

    private byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

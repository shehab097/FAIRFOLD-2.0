package com.fairfold.resume;

import com.fairfold.common.storage.StorageService;
import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private ResumeProcessingService processingService;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void Fr010_upload_valid_pdf() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        when(storageService.save(any(byte[].class), anyString())).thenAnswer(inv -> "key-" + UUID.randomUUID());

        byte[] pdf = "%PDF-1.4\n%fake pdf content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/candidate/resume").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("cv.pdf"))
                .andExpect(jsonPath("$.status").value("processing"))
                .andExpect(jsonPath("$.sizeBytes").value(pdf.length));

        mockMvc.perform(get("/api/candidate/resume").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("cv.pdf"));
    }

    @Test
    void Fr010_rejects_non_pdf_with_pdf_name() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        byte[] notPdf = "just some text".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", notPdf);

        mockMvc.perform(multipart("/api/candidate/resume").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
    }

    @Test
    void Fr010_rejects_over_10mb() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        big[0] = '%';
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", big);

        mockMvc.perform(multipart("/api/candidate/resume").file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void Fr010_new_upload_replaces_old() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        when(storageService.save(any(byte[].class), anyString())).thenAnswer(inv -> "key-" + UUID.randomUUID());

        byte[] pdf = "%PDF-1.4 content".getBytes();
        MockMultipartFile first = new MockMultipartFile("file", "first.pdf", "application/pdf", pdf);
        mockMvc.perform(multipart("/api/candidate/resume").file(first)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated());

        MockMultipartFile second = new MockMultipartFile("file", "second.pdf", "application/pdf", pdf);
        mockMvc.perform(multipart("/api/candidate/resume").file(second)
                        .header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isCreated());

        Integer currentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resumes WHERE user_id = ? AND is_current = true", Integer.class,
                candidate.userId().toString());
        org.junit.jupiter.api.Assertions.assertEquals(1, currentCount);

        mockMvc.perform(get("/api/candidate/resume").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("second.pdf"));
    }

    @Test
    void Fr010_no_resume_returns_404() throws Exception {
        TestUser candidate = registerUser("cand@example.com", "candidate");
        mockMvc.perform(get("/api/candidate/resume").header(HttpHeaders.AUTHORIZATION, bearer(candidate.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }
}

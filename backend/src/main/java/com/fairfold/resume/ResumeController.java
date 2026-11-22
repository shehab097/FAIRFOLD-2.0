package com.fairfold.resume;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.resume.dto.ApplySuggestionsRequest;
import com.fairfold.resume.dto.ResumeResponse;
import com.fairfold.resume.dto.StripReportRequest;
import com.fairfold.resume.dto.SuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidate/resume")
@Tag(name = "Candidate Resume", description = "Resume upload, stripping and extracted profile suggestions")
@PreAuthorize("hasRole('CANDIDATE')")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeSuggestionService suggestionService;

    public ResumeController(ResumeService resumeService, ResumeSuggestionService suggestionService) {
        this.resumeService = resumeService;
        this.suggestionService = suggestionService;
    }

    @PostMapping
    @Operation(summary = "Upload a PDF resume and start processing (replaces the current one)")
    public ResponseEntity<ResumeResponse> upload(@AuthenticationPrincipal CurrentUser user,
                                                 @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.upload(user.id(), file));
    }

    @GetMapping
    @Operation(summary = "Get the current resume, stripped text and placeholder summary")
    public ResumeResponse getCurrent(@AuthenticationPrincipal CurrentUser user) {
        return resumeService.getCurrent(user.id());
    }

    @GetMapping("/file")
    @Operation(summary = "Preview or download the current PDF resume")
    public ResponseEntity<byte[]> getCurrentFile(@AuthenticationPrincipal CurrentUser user) {
        ResumeService.DownloadedResume file = resumeService.loadCurrentFile(user.id());
        String safeName = file.filename().replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                .body(file.bytes());
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get the profile suggestions extracted from the resume")
    public SuggestionResponse suggestions(@AuthenticationPrincipal CurrentUser user) {
        return suggestionService.get(user.id());
    }

    @PostMapping("/suggestions/apply")
    @Operation(summary = "Save the reviewed suggestions to the profile, skills and work history")
    public ResponseEntity<Void> applySuggestions(@AuthenticationPrincipal CurrentUser user,
                                                 @RequestBody ApplySuggestionsRequest request) {
        suggestionService.apply(user.id(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/strip-reports")
    @Operation(summary = "Report a wrong strip")
    public ResponseEntity<Void> reportStrip(@AuthenticationPrincipal CurrentUser user,
                                            @RequestBody StripReportRequest request) {
        suggestionService.reportStrip(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

package com.fairfold.application;

import com.fairfold.application.dto.CandidateApplicationDetail;
import com.fairfold.application.dto.CandidateApplicationListItem;
import com.fairfold.application.dto.MyApplicationResponse;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Applications", description = "Applying to jobs and tracking application status")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/jobs/{id}/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply to an active job")
    public ResponseEntity<CandidateApplicationDetail> apply(@AuthenticationPrincipal CurrentUser user,
                                                            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.apply(user.id(), id));
    }

    @GetMapping("/jobs/{id}/applications/mine")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get the caller's application for a job, if any")
    public MyApplicationResponse mine(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return applicationService.myApplication(user.id(), id);
    }

    @GetMapping("/candidate/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "List the candidate's applications")
    public PageResponse<CandidateApplicationListItem> list(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.listForCandidate(user.id(), status, pageable);
    }

    @GetMapping("/candidate/applications/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get one application with its status history")
    public CandidateApplicationDetail get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return applicationService.getForCandidate(user.id(), id);
    }
}

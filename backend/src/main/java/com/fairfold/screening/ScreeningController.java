package com.fairfold.screening;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.screening.dto.ScreeningEstimateResponse;
import com.fairfold.screening.dto.ScreeningProgressResponse;
import com.fairfold.screening.dto.ScreeningRetryResponse;
import com.fairfold.screening.dto.ScreeningStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/jobs")
@Tag(name = "Employer Screening", description = "Run and monitor anonymised applicant screening")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @PostMapping("/{id}/screening/estimate")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Estimate the number of applicants and AI cost")
    public ScreeningEstimateResponse estimate(@AuthenticationPrincipal CurrentUser user,
                                              @PathVariable UUID id) {
        return screeningService.estimate(user.companyId(), id);
    }

    @PostMapping("/{id}/screening")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Start screening all new applicants")
    public ResponseEntity<ScreeningStartResponse> start(@AuthenticationPrincipal CurrentUser user,
                                                        @PathVariable UUID id) {
        return ResponseEntity.accepted()
                .body(screeningService.start(user.companyId(), user.id(), id));
    }

    @GetMapping("/{id}/screening")
    @Operation(summary = "Get the latest screening run's progress")
    public ScreeningProgressResponse progress(@AuthenticationPrincipal CurrentUser user,
                                              @PathVariable UUID id) {
        return screeningService.progress(user.companyId(), id);
    }

    @PostMapping("/{id}/screening/retry")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Retry failed screening items")
    public ScreeningRetryResponse retry(@AuthenticationPrincipal CurrentUser user,
                                        @PathVariable UUID id) {
        return new ScreeningRetryResponse(screeningService.retry(user.companyId(), id));
    }
}

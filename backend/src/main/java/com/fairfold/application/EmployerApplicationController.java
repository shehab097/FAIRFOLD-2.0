package com.fairfold.application;

import com.fairfold.application.dto.EmployerApplicantListItem;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.screening.EmployerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/jobs")
@Tag(name = "Employer Applicants", description = "Ranked applicant list for an employer's job")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class EmployerApplicationController {

    private final EmployerReviewService reviewService;

    public EmployerApplicationController(EmployerReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{jobId}/applications")
    @Operation(summary = "List applicants ranked by score (aliases only)")
    public PageResponse<EmployerApplicantListItem> list(@AuthenticationPrincipal CurrentUser user,
                                                        @PathVariable UUID jobId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String gate,
                                                        @RequestParam(required = false) String bias,
                                                        @RequestParam(defaultValue = "false") boolean includeNotMatched,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.list(user.companyId(), jobId, status, gate, bias, includeNotMatched, pageable);
    }
}

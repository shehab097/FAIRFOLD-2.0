package com.fairfold.application;

import com.fairfold.application.dto.EmployerApplicantListItem;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/jobs")
@Tag(name = "Employer Applicants", description = "Applicant list for an employer's job")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class EmployerApplicationController {

    private final ApplicationService applicationService;

    public EmployerApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{jobId}/applications")
    @Operation(summary = "List applicants (anonymised) for a job")
    public PageResponse<EmployerApplicantListItem> list(@AuthenticationPrincipal CurrentUser user,
                                                        @PathVariable UUID jobId,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.listForEmployer(user.companyId(), jobId, pageable);
    }
}

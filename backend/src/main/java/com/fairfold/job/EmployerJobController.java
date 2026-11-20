package com.fairfold.job;

import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.job.dto.EmployerJobDetail;
import com.fairfold.job.dto.EmployerJobListItem;
import com.fairfold.job.dto.JobRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/jobs")
@Tag(name = "Employer Jobs", description = "Job management for employers")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class EmployerJobController {

    private final JobService jobService;

    public EmployerJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "List the caller's company jobs")
    public PageResponse<EmployerJobListItem> list(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return jobService.listEmployerJobs(user.companyId(), status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job (scoped to the caller's company)")
    public EmployerJobDetail get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return jobService.getEmployerJob(user.companyId(), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Create a draft job")
    public ResponseEntity<EmployerJobDetail> create(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.create(user.companyId(), user.id(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Update a draft job")
    public EmployerJobDetail update(
            @AuthenticationPrincipal CurrentUser user,
            @PathVariable UUID id,
            @Valid @RequestBody JobRequest request) {
        return jobService.update(user.companyId(), user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Delete a draft job")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        jobService.delete(user.companyId(), user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Activate a draft job")
    public EmployerJobDetail activate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return jobService.activate(user.companyId(), user.id(), id);
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Pause an active job")
    public EmployerJobDetail pause(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return jobService.pause(user.companyId(), user.id(), id);
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Resume a paused job")
    public EmployerJobDetail resume(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return jobService.resume(user.companyId(), user.id(), id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Close a job (final)")
    public EmployerJobDetail close(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return jobService.close(user.companyId(), user.id(), id);
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Duplicate a job as a new draft")
    public EmployerJobDetail duplicate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return jobService.duplicate(user.companyId(), user.id(), id);
    }
}

package com.fairfold.interview;

import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.interview.dto.InterviewDetail;
import com.fairfold.interview.dto.InterviewFeedbackDto;
import com.fairfold.interview.dto.InterviewFeedbackRequest;
import com.fairfold.interview.dto.InterviewListItem;
import com.fairfold.interview.dto.ScheduleInterviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api")
@Tag(name = "Interviews", description = "Schedule interviews, submit feedback and hire")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/employer/applications/{id}/interviews")
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
    @Operation(summary = "Schedule an interview for a shortlisted application")
    public InterviewDetail schedule(@AuthenticationPrincipal CurrentUser user,
                                    @PathVariable UUID id,
                                    @Valid @RequestBody ScheduleInterviewRequest request) {
        return interviewService.schedule(user.companyId(), user.id(), id, request);
    }

    @PostMapping("/employer/applications/{id}/hire")
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
    @Operation(summary = "Mark an interviewed candidate as hired")
    public ResponseEntity<Void> hire(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        interviewService.hire(user.companyId(), user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/interviews")
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER', 'INTERVIEWER')")
    @Operation(summary = "List upcoming or past interviews")
    public PageResponse<InterviewListItem> list(@AuthenticationPrincipal CurrentUser user,
                                                @RequestParam(defaultValue = "upcoming") String tab,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return interviewService.list(user.companyId(), user.id(), user.role(), tab, pageable);
    }

    @GetMapping("/interviews/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER', 'INTERVIEWER')")
    @Operation(summary = "Interview detail with pack and feedback")
    public InterviewDetail detail(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return interviewService.detail(user.companyId(), user.id(), user.role(), id);
    }

    @PutMapping("/interviews/{id}/feedback")
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER', 'INTERVIEWER')")
    @Operation(summary = "Create or update the caller's interview feedback")
    public InterviewFeedbackDto feedback(@AuthenticationPrincipal CurrentUser user,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody InterviewFeedbackRequest request) {
        return interviewService.submitFeedback(
                user.companyId(), user.id(), user.role(), id, request);
    }
}

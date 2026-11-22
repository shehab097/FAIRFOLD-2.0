package com.fairfold.screening;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.screening.dto.DecisionRequest;
import com.fairfold.screening.dto.EmployerReview;
import com.fairfold.screening.dto.ScreeningStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/applications")
@Tag(name = "Employer Review", description = "Anonymised application review, shortlist, reject and pull back")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class EmployerReviewController {

    private final EmployerReviewService reviewService;

    public EmployerReviewController(EmployerReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get the anonymised review (identity only after shortlist)")
    public EmployerReview review(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return reviewService.review(user.companyId(), id);
    }

    @PostMapping("/{id}/shortlist")
    @Operation(summary = "Shortlist an application (reveals identity and writes an audit entry)")
    public EmployerReview shortlist(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                    @RequestBody(required = false) DecisionRequest request) {
        return reviewService.shortlist(user.companyId(), user.id(), id, request);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an application")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
                                       @RequestBody(required = false) DecisionRequest request) {
        reviewService.reject(user.companyId(), user.id(), id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pull-back")
    @Operation(summary = "Pull a not-matched candidate back into screening (skips hard filters)")
    public ResponseEntity<ScreeningStartResponse> pullBack(@AuthenticationPrincipal CurrentUser user,
                                                           @PathVariable UUID id) {
        return ResponseEntity.accepted()
                .body(reviewService.pullBack(user.companyId(), user.id(), id));
    }
}

package com.fairfold.screening;

import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.screening.dto.ShortlistedCandidate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/shortlisted")
@Tag(name = "Employer Shortlist", description = "All shortlisted candidates across the company's jobs")
public class ShortlistController {

    private final ShortlistService shortlistService;

    public ShortlistController(ShortlistService shortlistService) {
        this.shortlistService = shortlistService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
    @Operation(summary = "List every shortlisted candidate for the caller's company")
    public PageResponse<ShortlistedCandidate> list(@AuthenticationPrincipal CurrentUser user,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return shortlistService.list(user.companyId(), pageable);
    }
}

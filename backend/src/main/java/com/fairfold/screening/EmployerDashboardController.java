package com.fairfold.screening;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.screening.dto.EmployerDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/dashboard")
@Tag(name = "Employer Dashboard", description = "Employer dashboard counters")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class EmployerDashboardController {

    private final EmployerDashboardService dashboardService;

    public EmployerDashboardController(EmployerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get employer dashboard counters")
    public EmployerDashboardResponse dashboard(@AuthenticationPrincipal CurrentUser user) {
        return dashboardService.dashboard(user.companyId());
    }
}

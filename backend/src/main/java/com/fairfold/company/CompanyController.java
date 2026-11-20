package com.fairfold.company;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.company.dto.CompanyProfileRequest;
import com.fairfold.company.dto.CompanyProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employer/company")
@Tag(name = "Company", description = "Employer company profile")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Get the caller's company profile")
    public CompanyProfileResponse get(@AuthenticationPrincipal CurrentUser user) {
        return companyService.get(user.companyId());
    }

    @PutMapping
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Update the caller's company profile")
    public CompanyProfileResponse update(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody CompanyProfileRequest request) {
        return companyService.update(user.companyId(), request);
    }
}

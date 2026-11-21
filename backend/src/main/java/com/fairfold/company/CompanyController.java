package com.fairfold.company;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.company.dto.CompanyProfileRequest;
import com.fairfold.company.dto.CompanyProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Upload a company logo image")
    public CompanyProfileResponse uploadLogo(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam("file") MultipartFile file) {
        return companyService.uploadLogo(user.companyId(), file);
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Remove the company logo")
    public CompanyProfileResponse deleteLogo(@AuthenticationPrincipal CurrentUser user) {
        return companyService.deleteLogo(user.companyId());
    }
}

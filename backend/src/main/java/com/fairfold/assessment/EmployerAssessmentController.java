package com.fairfold.assessment;

import com.fairfold.assessment.dto.EmployerAssessmentOption;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employer/assessments")
@Tag(name = "Employer Assessments", description = "Active assessments an employer can require")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class EmployerAssessmentController {

    private final AssessmentService assessmentService;

    public EmployerAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    @Operation(summary = "List active assessments for job requirements")
    public List<EmployerAssessmentOption> list(@AuthenticationPrincipal CurrentUser user) {
        return assessmentService.listActiveOptions();
    }
}

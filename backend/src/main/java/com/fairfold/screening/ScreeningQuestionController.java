package com.fairfold.screening;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.screening.dto.ScreeningQuestionsRequest;
import com.fairfold.screening.dto.ScreeningQuestionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/jobs")
@Tag(name = "Employer Screening Questions", description = "AI-assisted screening questions for a job")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class ScreeningQuestionController {

    private final ScreeningQuestionService questionService;

    public ScreeningQuestionController(ScreeningQuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping("/{id}/suggest-questions")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Suggest screening questions (AI with rule-based fallback)")
    public ScreeningQuestionsResponse suggest(@AuthenticationPrincipal CurrentUser user,
                                              @PathVariable UUID id) {
        return questionService.suggest(user.companyId(), user.id(), id);
    }

    @PutMapping("/{id}/screening-questions")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Replace a draft job's screening questions")
    public ScreeningQuestionsResponse save(@AuthenticationPrincipal CurrentUser user,
                                           @PathVariable UUID id,
                                           @RequestBody ScreeningQuestionsRequest request) {
        return questionService.save(user.companyId(), id, request);
    }
}

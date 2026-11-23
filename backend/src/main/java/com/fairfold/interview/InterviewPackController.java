package com.fairfold.interview;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.interview.dto.InterviewPackDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employer/jobs")
@Tag(name = "Interview Pack", description = "AI-assisted interview questions and rubric")
@PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class InterviewPackController {

    private final InterviewPackService packService;

    public InterviewPackController(InterviewPackService packService) {
        this.packService = packService;
    }

    @GetMapping("/{id}/interview-pack")
    @Operation(summary = "Get the saved interview pack")
    public InterviewPackDto get(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return packService.get(user.companyId(), id);
    }

    @PostMapping("/{id}/interview-pack/generate")
    @Operation(summary = "Propose an interview pack with AI (not saved)")
    public InterviewPackDto generate(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return packService.generate(user.companyId(), user.id(), id);
    }

    @PutMapping("/{id}/interview-pack")
    @Operation(summary = "Save the interview pack")
    public InterviewPackDto save(@AuthenticationPrincipal CurrentUser user,
                                 @PathVariable UUID id,
                                 @RequestBody InterviewPackDto.SaveRequest request) {
        return packService.save(user.companyId(), user.id(), id, request);
    }
}

package com.fairfold.candidate;

import com.fairfold.candidate.dto.JourneyResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidate/journey")
@Tag(name = "Candidate Journey", description = "Career timeline and skill evolution")
@PreAuthorize("hasRole('CANDIDATE')")
public class JourneyController {

    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @GetMapping
    @Operation(summary = "Career timeline and skill evolution")
    public JourneyResponse journey(@AuthenticationPrincipal CurrentUser user) {
        return journeyService.journey(user.id());
    }
}

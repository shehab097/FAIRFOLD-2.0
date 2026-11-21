package com.fairfold.candidate;

import com.fairfold.candidate.dto.AddSkillRequest;
import com.fairfold.candidate.dto.CandidateSkillResponse;
import com.fairfold.candidate.dto.DashboardResponse;
import com.fairfold.candidate.dto.ProfileRequest;
import com.fairfold.candidate.dto.ProfileResponse;
import com.fairfold.candidate.dto.UpdateSkillRequest;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidate")
@Tag(name = "Candidate", description = "Candidate profile, skills and dashboard")
@PreAuthorize("hasRole('CANDIDATE')")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get the candidate profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal CurrentUser user) {
        return candidateService.getProfile(user.id());
    }

    @PutMapping("/profile")
    @Operation(summary = "Save the candidate profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal CurrentUser user,
                                         @RequestBody ProfileRequest request) {
        return candidateService.updateProfile(user.id(), request);
    }

    @GetMapping("/skills")
    @Operation(summary = "List the candidate's skills")
    public List<CandidateSkillResponse> listSkills(@AuthenticationPrincipal CurrentUser user) {
        return candidateService.listSkills(user.id());
    }

    @PostMapping("/skills")
    @Operation(summary = "Add a skill")
    public ResponseEntity<CandidateSkillResponse> addSkill(@AuthenticationPrincipal CurrentUser user,
                                                           @RequestBody AddSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidateService.addSkill(user.id(), request));
    }

    @PutMapping("/skills/{id}")
    @Operation(summary = "Update a skill")
    public CandidateSkillResponse updateSkill(@AuthenticationPrincipal CurrentUser user,
                                              @PathVariable UUID id,
                                              @RequestBody UpdateSkillRequest request) {
        return candidateService.updateSkill(user.id(), id, request);
    }

    @DeleteMapping("/skills/{id}")
    @Operation(summary = "Delete a skill")
    public ResponseEntity<Void> deleteSkill(@AuthenticationPrincipal CurrentUser user,
                                            @PathVariable UUID id) {
        candidateService.deleteSkill(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Candidate dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal CurrentUser user) {
        return candidateService.dashboard(user.id());
    }
}

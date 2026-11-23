package com.fairfold.team;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.team.dto.InviteRequest;
import com.fairfold.team.dto.TeamResponse;
import com.fairfold.team.dto.TeamRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employer/team")
@Tag(name = "Employer Team", description = "Company members and invites")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "List members and pending invites")
    public TeamResponse list(@AuthenticationPrincipal CurrentUser user) {
        return teamService.list(user.companyId());
    }

    @PostMapping("/invites")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Invite a team member")
    public ResponseEntity<TeamResponse.Invite> invite(@AuthenticationPrincipal CurrentUser user,
                                                      @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.invite(user.companyId(), user.id(), request.email(), request.role()));
    }

    @DeleteMapping("/invites/{id}")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Cancel a pending invite")
    public ResponseEntity<Void> cancelInvite(@AuthenticationPrincipal CurrentUser user,
                                             @PathVariable UUID id) {
        teamService.cancelInvite(user.companyId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/members/{id}")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Change a member's role")
    public TeamResponse.Member changeRole(@AuthenticationPrincipal CurrentUser user,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody TeamRoleRequest request) {
        return teamService.changeRole(user.companyId(), user.id(), id, request.role());
    }

    @DeleteMapping("/members/{id}")
    @PreAuthorize("hasRole('EMPLOYER_HR')")
    @Operation(summary = "Remove a member")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CurrentUser user,
                                       @PathVariable UUID id) {
        teamService.remove(user.companyId(), user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/interviewers")
    @PreAuthorize("hasAnyRole('EMPLOYER_HR', 'EMPLOYER_MANAGER')")
    @Operation(summary = "List active members who can interview")
    public List<TeamResponse.Member> interviewers(@AuthenticationPrincipal CurrentUser user) {
        return teamService.members(user.companyId());
    }
}

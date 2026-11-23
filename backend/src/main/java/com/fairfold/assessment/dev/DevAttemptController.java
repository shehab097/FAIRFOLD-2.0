package com.fairfold.assessment.dev;

import com.fairfold.assessment.dto.AttemptView;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Development-only assessment shortcut. Hidden outside the local/test profiles. */
@RestController
@RequestMapping("/api/dev/attempts")
@Profile({"local", "test"})
@Tag(name = "Dev Tools", description = "Development-only shortcuts (not part of the final build)")
@PreAuthorize("hasRole('CANDIDATE')")
public class DevAttemptController {

    private final DevAttemptService devAttemptService;

    public DevAttemptController(DevAttemptService devAttemptService) {
        this.devAttemptService = devAttemptService;
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Fill an attempt with N correct answers and submit (dev only)")
    public AttemptView complete(@AuthenticationPrincipal CurrentUser user,
                                @PathVariable UUID id,
                                @RequestBody(required = false) DevCompleteRequest request) {
        return devAttemptService.complete(user.id(), id,
                request == null ? null : request.correctCount());
    }
}

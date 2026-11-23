package com.fairfold.coaching;

import com.fairfold.coaching.dto.PracticeQuestionOption;
import com.fairfold.coaching.dto.PracticeRatingRequest;
import com.fairfold.coaching.dto.PracticeSessionListItem;
import com.fairfold.coaching.dto.PracticeSessionRequest;
import com.fairfold.coaching.dto.PracticeSessionResponse;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/practice")
@Tag(name = "Practice", description = "Private STAR interview practice")
@PreAuthorize("hasRole('CANDIDATE')")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @GetMapping("/questions")
    @Operation(summary = "List active practice questions, optionally by topic")
    public List<PracticeQuestionOption> questions(@RequestParam(required = false) String topic) {
        return practiceService.questions(topic);
    }

    @PostMapping("/sessions")
    @Operation(summary = "Answer a practice question and request STAR feedback")
    public ResponseEntity<PracticeSessionResponse> create(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody PracticeSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(practiceService.createSession(user.id(), request));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List the candidate's practice sessions")
    public PageResponse<PracticeSessionListItem> history(@AuthenticationPrincipal CurrentUser user,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return practiceService.history(user.id(), pageable);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get one private practice session")
    public PracticeSessionResponse session(@AuthenticationPrincipal CurrentUser user,
                                           @PathVariable UUID id) {
        return practiceService.session(user.id(), id);
    }

    @PostMapping("/sessions/{id}/rating")
    @Operation(summary = "Rate the helpfulness of a practice session (1–5)")
    public PracticeSessionResponse rate(@AuthenticationPrincipal CurrentUser user,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody PracticeRatingRequest request) {
        return practiceService.rate(user.id(), id, request.rating());
    }
}

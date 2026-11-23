package com.fairfold.assessment;

import com.fairfold.assessment.dto.AttemptHistoryItem;
import com.fairfold.assessment.dto.AttemptView;
import com.fairfold.assessment.dto.CandidateAssessment;
import com.fairfold.assessment.dto.CandidateResource;
import com.fairfold.assessment.dto.ConsentRequest;
import com.fairfold.assessment.dto.ExplanationRequest;
import com.fairfold.assessment.dto.ProctorEventRequest;
import com.fairfold.assessment.dto.ProctorEventsResponse;
import com.fairfold.assessment.dto.SaveAnswersRequest;
import com.fairfold.assessment.dto.SaveAnswersResponse;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Candidate Assessments", description = "Take assessments, autosave answers and see results")
@PreAuthorize("hasRole('CANDIDATE')")
public class AssessmentAttemptController {

    private final AttemptService attemptService;
    private final ProctoringService proctoringService;
    private final Clock clock;

    public AssessmentAttemptController(AttemptService attemptService,
                                       ProctoringService proctoringService, Clock clock) {
        this.attemptService = attemptService;
        this.proctoringService = proctoringService;
        this.clock = clock;
    }

    @GetMapping("/assessments")
    @Operation(summary = "List active assessments with the candidate's progress")
    public List<CandidateAssessment> list(@AuthenticationPrincipal CurrentUser user) {
        return attemptService.listAssessments(user.id());
    }

    @GetMapping("/candidate/attempts")
    @Operation(summary = "List the candidate's submitted attempts")
    public PageResponse<AttemptHistoryItem> history(@AuthenticationPrincipal CurrentUser user,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return attemptService.history(user.id(), pageable);
    }

    @PostMapping("/assessments/{id}/attempts")
    @Operation(summary = "Start a new attempt or resume one in progress")
    public AttemptView start(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return attemptService.start(user.id(), id);
    }

    @PutMapping("/attempts/{id}/answers")
    @Operation(summary = "Merge autosaved answers into an in-progress attempt")
    public SaveAnswersResponse saveAnswers(@AuthenticationPrincipal CurrentUser user,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody SaveAnswersRequest request) {
        Map<String, String> saved = attemptService.save(user.id(), id, request.answers());
        return new SaveAnswersResponse(clock.instant(), saved);
    }

    @PostMapping("/attempts/{id}/submit")
    @Operation(summary = "Submit an attempt (idempotent) and score it")
    public AttemptView submit(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return attemptService.submit(user.id(), id);
    }

    @GetMapping("/attempts/{id}")
    @Operation(summary = "Get an in-progress attempt or its result")
    public AttemptView view(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return attemptService.view(user.id(), id);
    }

    @PostMapping("/attempts/{id}/consent")
    @Operation(summary = "Record proctoring consent once, before answering")
    public Map<String, Boolean> consent(@AuthenticationPrincipal CurrentUser user,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody ConsentRequest request) {
        return Map.of("given", proctoringService.consent(user.id(), id, request.given()));
    }

    @PostMapping("/attempts/{id}/events")
    @Operation(summary = "Record focus signals (ignored without consent; max 500 per attempt)")
    public ProctorEventsResponse events(@AuthenticationPrincipal CurrentUser user,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody List<@Valid ProctorEventRequest> events) {
        return proctoringService.addEvents(user.id(), id, events);
    }

    @PutMapping("/attempts/{id}/explanation")
    @Operation(summary = "Set the candidate's explanation (only with consent)")
    public Map<String, String> explanation(@AuthenticationPrincipal CurrentUser user,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody ExplanationRequest request) {
        return Collections.singletonMap("text",
                proctoringService.explanation(user.id(), id, request.text()));
    }

    @GetMapping("/resources")
    @Operation(summary = "List active, working learning resources for a skill")
    public List<CandidateResource> resources(@RequestParam UUID skillId) {
        return attemptService.resources(skillId);
    }
}

package com.fairfold.assessment;

import com.fairfold.assessment.dto.ActiveRequest;
import com.fairfold.assessment.dto.AssessmentDetail;
import com.fairfold.assessment.dto.AssessmentListItem;
import com.fairfold.assessment.dto.AssessmentRequest;
import com.fairfold.assessment.dto.LinkCheckResult;
import com.fairfold.assessment.dto.LearningResourceDto;
import com.fairfold.assessment.dto.ResourceRequest;
import com.fairfold.assessment.dto.SkillMergeRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Content", description = "Assessments, questions and learning resources")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final AssessmentService assessmentService;
    private final LearningResourceService resourceService;
    private final ResourceLinkChecker linkChecker;
    private final SkillMergeService mergeService;
    private final SkillRescanService rescanService;

    public AdminContentController(AssessmentService assessmentService,
                                  LearningResourceService resourceService,
                                  ResourceLinkChecker linkChecker,
                                  SkillMergeService mergeService,
                                  SkillRescanService rescanService) {
        this.assessmentService = assessmentService;
        this.resourceService = resourceService;
        this.linkChecker = linkChecker;
        this.mergeService = mergeService;
        this.rescanService = rescanService;
    }

    // --- assessments -------------------------------------------------------

    @GetMapping("/assessments")
    @Operation(summary = "List assessments")
    public PageResponse<AssessmentListItem> listAssessments(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return assessmentService.list(pageable);
    }

    @GetMapping("/assessments/{id}")
    @Operation(summary = "Get an assessment with its questions")
    public AssessmentDetail getAssessment(@PathVariable UUID id) {
        return assessmentService.detail(id);
    }

    @PostMapping("/assessments")
    @Operation(summary = "Create an assessment")
    public ResponseEntity<AssessmentDetail> createAssessment(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody AssessmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.create(user.id(), request));
    }

    @PutMapping("/assessments/{id}")
    @Operation(summary = "Update an assessment (questions lock once attempts exist)")
    public AssessmentDetail updateAssessment(@AuthenticationPrincipal CurrentUser user,
                                             @PathVariable UUID id,
                                             @Valid @RequestBody AssessmentRequest request) {
        return assessmentService.update(user.id(), id, request);
    }

    @PatchMapping("/assessments/{id}/active")
    @Operation(summary = "Activate or deactivate an assessment")
    public AssessmentDetail setAssessmentActive(@AuthenticationPrincipal CurrentUser user,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody ActiveRequest request) {
        return assessmentService.setActive(user.id(), id, request);
    }

    @PostMapping("/assessments/{id}/generate")
    @Operation(summary = "Generate a question pool and move to pending review")
    public AssessmentDetail generate(@AuthenticationPrincipal CurrentUser user,
                                     @PathVariable UUID id,
                                     @RequestParam(defaultValue = "30") int count) {
        return assessmentService.generate(user.id(), id, count);
    }

    @PostMapping("/assessments/{id}/approve")
    @Operation(summary = "Approve an assessment and activate it")
    public AssessmentDetail approve(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return assessmentService.approve(user.id(), id);
    }

    @PostMapping("/skills/{sourceSkillId}/merge")
    @Operation(summary = "Merge a duplicate skill into a canonical skill")
    public SkillMergeService.MergeResult mergeSkill(@AuthenticationPrincipal CurrentUser user,
                                                    @PathVariable UUID sourceSkillId,
                                                    @Valid @RequestBody SkillMergeRequest request) {
        return mergeService.merge(sourceSkillId, request.targetSkillId(), user.id());
    }

    @PostMapping("/skills/rescan")
    @Operation(summary = "Trim skill names and auto-merge alias-shaped duplicates")
    public SkillRescanService.RescanResult rescanSkills(@AuthenticationPrincipal CurrentUser user) {
        return rescanService.rescan(user.id());
    }

    // --- resources ---------------------------------------------------------

    @GetMapping("/resources")
    @Operation(summary = "List learning resources")
    public PageResponse<LearningResourceDto> listResources(
            @RequestParam(required = false) UUID skillId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return resourceService.list(skillId, pageable);
    }

    @PostMapping("/resources")
    @Operation(summary = "Create a learning resource")
    public ResponseEntity<LearningResourceDto> createResource(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.create(user.id(), request));
    }

    @PutMapping("/resources/{id}")
    @Operation(summary = "Update a learning resource")
    public LearningResourceDto updateResource(@AuthenticationPrincipal CurrentUser user,
                                              @PathVariable UUID id,
                                              @Valid @RequestBody ResourceRequest request) {
        return resourceService.update(user.id(), id, request);
    }

    @PatchMapping("/resources/{id}/active")
    @Operation(summary = "Activate or deactivate a learning resource")
    public LearningResourceDto setResourceActive(@AuthenticationPrincipal CurrentUser user,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody ActiveRequest request) {
        return resourceService.setActive(user.id(), id, request);
    }

    @PostMapping("/resources/check")
    @Operation(summary = "Check every resource link now")
    public LinkCheckResult checkLinks() {
        return linkChecker.checkAll();
    }
}

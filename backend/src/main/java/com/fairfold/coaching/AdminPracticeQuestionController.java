package com.fairfold.coaching;

import com.fairfold.coaching.dto.PracticeActiveRequest;
import com.fairfold.coaching.dto.PracticeQuestionDto;
import com.fairfold.coaching.dto.PracticeQuestionRequest;
import com.fairfold.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/practice-questions")
@Tag(name = "Admin Practice Questions", description = "Interview-practice question bank")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPracticeQuestionController {

    private final PracticeService practiceService;

    public AdminPracticeQuestionController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @GetMapping
    @Operation(summary = "List practice questions (including inactive)")
    public PageResponse<PracticeQuestionDto> list(
            @PageableDefault(size = 20, sort = "topic") Pageable pageable) {
        return practiceService.listQuestions(pageable);
    }

    @PostMapping
    @Operation(summary = "Add a practice question")
    public ResponseEntity<PracticeQuestionDto> create(
            @Valid @RequestBody PracticeQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(practiceService.createQuestion(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a practice question")
    public PracticeQuestionDto update(@PathVariable UUID id,
                                      @Valid @RequestBody PracticeQuestionRequest request) {
        return practiceService.updateQuestion(id, request);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a practice question")
    public PracticeQuestionDto setActive(@PathVariable UUID id,
                                         @Valid @RequestBody PracticeActiveRequest request) {
        return practiceService.setQuestionActive(id, request.active());
    }
}

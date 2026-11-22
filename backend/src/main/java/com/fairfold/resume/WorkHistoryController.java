package com.fairfold.resume;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.resume.dto.WorkHistoryRequest;
import com.fairfold.resume.dto.WorkHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/candidate/work-history")
@Tag(name = "Candidate Work History", description = "Candidate work history entries")
@PreAuthorize("hasRole('CANDIDATE')")
public class WorkHistoryController {

    private final WorkHistoryService workHistoryService;

    public WorkHistoryController(WorkHistoryService workHistoryService) {
        this.workHistoryService = workHistoryService;
    }

    @GetMapping
    @Operation(summary = "List work history")
    public List<WorkHistoryResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return workHistoryService.list(user.id());
    }

    @PostMapping
    @Operation(summary = "Add a work history entry")
    public ResponseEntity<WorkHistoryResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                      @RequestBody WorkHistoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workHistoryService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a work history entry")
    public WorkHistoryResponse update(@AuthenticationPrincipal CurrentUser user,
                                      @PathVariable UUID id,
                                      @RequestBody WorkHistoryRequest request) {
        return workHistoryService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a work history entry")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user,
                                       @PathVariable UUID id) {
        workHistoryService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}

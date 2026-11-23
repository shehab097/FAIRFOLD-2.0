package com.fairfold.assessment;

import com.fairfold.assessment.dto.CertificationRequest;
import com.fairfold.assessment.dto.CertificationResponse;
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
@RequestMapping("/api/candidate/certifications")
@Tag(name = "Candidate Certifications", description = "Candidate-managed certifications")
@PreAuthorize("hasRole('CANDIDATE')")
public class CertificationController {

    private final CertificationService certificationService;

    public CertificationController(CertificationService certificationService) {
        this.certificationService = certificationService;
    }

    @GetMapping
    @Operation(summary = "List certifications")
    public List<CertificationResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return certificationService.list(user.id());
    }

    @PostMapping
    @Operation(summary = "Add a certification")
    public ResponseEntity<CertificationResponse> create(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody CertificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificationService.create(user.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a certification")
    public CertificationResponse update(@AuthenticationPrincipal CurrentUser user,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody CertificationRequest request) {
        return certificationService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a certification")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user,
                                       @PathVariable UUID id) {
        certificationService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}

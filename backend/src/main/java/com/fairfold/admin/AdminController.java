package com.fairfold.admin;

import com.fairfold.admin.dto.AdminStatsResponse;
import com.fairfold.admin.dto.AdminUserItem;
import com.fairfold.admin.dto.AuditLogItem;
import com.fairfold.admin.dto.RoleChangeRequest;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin dashboard, users and audit log")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminAuditService auditService;

    public AdminController(AdminService adminService, AdminAuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Admin dashboard counts and AI usage")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    @Operation(summary = "Search and filter users")
    public PageResponse<AdminUserItem> users(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.users(q, role, locked, page, size);
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change an employer team member's role")
    public AdminUserItem changeRole(@AuthenticationPrincipal CurrentUser user,
                                    @PathVariable UUID id,
                                    @RequestBody RoleChangeRequest request) {
        return adminService.changeRole(user.id(), id, request);
    }

    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "Clear a user's failed logins and lock")
    public AdminUserItem unlock(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id) {
        return adminService.unlock(user.id(), id);
    }

    @PostMapping("/users/{id}/password-reset")
    @Operation(summary = "Send a password reset link to a user")
    public ResponseEntity<Void> passwordReset(@AuthenticationPrincipal CurrentUser user,
                                              @PathVariable UUID id) {
        adminService.sendPasswordReset(user.id(), id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/users/{id}/reset-mfa")
    @Operation(summary = "Clear a user's MFA settings")
    public ResponseEntity<Void> resetMfa(@AuthenticationPrincipal CurrentUser user,
                                         @PathVariable UUID id) {
        adminService.resetMfa(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit")
    @Operation(summary = "Filter the audit log")
    public PageResponse<AuditLogItem> audit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.list(action, resourceType, actorId, from, to, page, size);
    }

    @GetMapping(value = "/audit", params = "format=csv", produces = "text/csv")
    @Operation(summary = "Export all matching audit rows as CSV (cap 50,000)")
    public void auditCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"fairfold-audit-" + LocalDate.now() + ".csv\"");
        auditService.exportCsv(action, resourceType, actorId, from, to, response.getWriter());
    }
}

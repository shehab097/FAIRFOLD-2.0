package com.fairfold.auth;

import com.fairfold.account.AccountErasureService;
import com.fairfold.account.AccountExportService;
import com.fairfold.account.DeleteAccountRequest;
import com.fairfold.auth.dto.ChangePasswordRequest;
import com.fairfold.auth.dto.UserDto;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.user.ProfilePhotoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Account", description = "Signed-in account management")
public class AccountController {

    private final AuthService authService;
    private final ProfilePhotoService profilePhotoService;
    private final AccountExportService exportService;
    private final AccountErasureService erasureService;
    private final ObjectMapper objectMapper;

    public AccountController(AuthService authService, ProfilePhotoService profilePhotoService,
                             AccountExportService exportService,
                             AccountErasureService erasureService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.profilePhotoService = profilePhotoService;
        this.exportService = exportService;
        this.erasureService = erasureService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the current password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER_HR')")
    @Operation(summary = "Upload or replace the current user's profile photo (480×480 WebP)")
    public UserDto uploadPhoto(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam("file") MultipartFile file) {
        return UserDto.from(profilePhotoService.upload(currentUser.id(), file));
    }

    @DeleteMapping("/photo")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER_HR')")
    @Operation(summary = "Remove the current user's profile photo")
    public UserDto deletePhoto(@AuthenticationPrincipal CurrentUser currentUser) {
        return UserDto.from(profilePhotoService.delete(currentUser.id()));
    }

    @GetMapping("/export")
    @Operation(summary = "Download all of the signed-in user's data as JSON")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal CurrentUser currentUser)
            throws IOException {
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(exportService.export(currentUser.id()));
        String filename = "fairfold-data-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(json);
    }

    @DeleteMapping
    @Operation(summary = "Delete the signed-in account after password and DELETE confirmation")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal CurrentUser currentUser,
                                              @RequestBody DeleteAccountRequest request) {
        erasureService.delete(currentUser.id(), request);
        return ResponseEntity.noContent().build();
    }
}

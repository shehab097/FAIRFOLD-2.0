package com.fairfold.auth;

import com.fairfold.auth.dto.ChangePasswordRequest;
import com.fairfold.auth.dto.UserDto;
import com.fairfold.common.security.CurrentUser;
import com.fairfold.user.ProfilePhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Account", description = "Signed-in account management")
public class AccountController {

    private final AuthService authService;
    private final ProfilePhotoService profilePhotoService;

    public AccountController(AuthService authService, ProfilePhotoService profilePhotoService) {
        this.authService = authService;
        this.profilePhotoService = profilePhotoService;
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
    @Operation(summary = "Upload or replace the current user's profile photo")
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
}

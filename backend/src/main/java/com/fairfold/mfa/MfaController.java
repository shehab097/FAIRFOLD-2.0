package com.fairfold.mfa;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.mfa.dto.MfaDisableRequest;
import com.fairfold.mfa.dto.MfaEnableRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/mfa")
@Tag(name = "MFA", description = "TOTP two-step login")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    @Operation(summary = "Start MFA setup and return the secret and QR code")
    public MfaService.SetupResponse setup(@AuthenticationPrincipal CurrentUser user) {
        return mfaService.setup(user.id());
    }

    @PostMapping("/enable")
    @Operation(summary = "Enable MFA with a code from the authenticator app")
    public ResponseEntity<Void> enable(@AuthenticationPrincipal CurrentUser user,
                                       @RequestBody MfaEnableRequest request) {
        mfaService.enable(user.id(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA with the password and a valid code")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal CurrentUser user,
                                        @RequestBody MfaDisableRequest request) {
        mfaService.disable(user.id(), request.password(), request.code());
        return ResponseEntity.noContent().build();
    }
}

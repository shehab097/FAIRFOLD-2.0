package com.fairfold.message;

import com.fairfold.common.security.CurrentUser;
import com.fairfold.message.dto.MessageDto;
import com.fairfold.message.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/applications")
@Tag(name = "Messages", description = "Application threads after shortlist")
@PreAuthorize("hasAnyRole('CANDIDATE', 'EMPLOYER_HR', 'EMPLOYER_MANAGER')")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Read the application thread and mark the other side's messages read")
    public List<MessageDto> thread(@AuthenticationPrincipal CurrentUser user,
                                   @PathVariable UUID id,
                                   @RequestParam(required = false) UUID afterId) {
        return messageService.thread(user.id(), user.companyId(), user.role(), id, afterId);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message on a shortlisted application")
    public ResponseEntity<MessageDto> send(@AuthenticationPrincipal CurrentUser user,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.send(user.id(), user.companyId(), user.role(), id, request.body()));
    }
}

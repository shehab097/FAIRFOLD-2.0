package com.fairfold.team.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamResponse(List<Member> members, List<Invite> invites) {

    public record Member(UUID id, String name, String email, String role) {
    }

    public record Invite(UUID id, String email, String role, Instant expiresAt) {
    }
}

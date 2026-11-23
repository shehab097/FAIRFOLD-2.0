package com.fairfold.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScheduleInterviewRequest(
        @NotNull Instant scheduledAt,
        @NotNull @Min(15) @Max(240) Integer durationMinutes,
        @NotBlank String meetingLink,
        @NotEmpty List<UUID> interviewerIds
) {
}

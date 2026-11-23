package com.fairfold.assessment;

import com.fairfold.assessment.dto.ProctorEventRequest;
import com.fairfold.assessment.dto.ProctorEventsResponse;
import com.fairfold.common.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Consent, focus signals and the candidate's explanation. Signals are stored
 * only after explicit consent and never touch scoring or the gate.
 */
@Service
public class ProctoringService {

    static final int MAX_EVENTS = 500;

    private final AssessmentAttemptRepository attemptRepository;
    private final ProctorEventRepository eventRepository;
    private final Clock clock;

    public ProctoringService(AssessmentAttemptRepository attemptRepository,
                             ProctorEventRepository eventRepository,
                             Clock clock) {
        this.attemptRepository = attemptRepository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    public Boolean consent(UUID userId, UUID attemptId, boolean given) {
        AssessmentAttempt attempt = requireOwned(userId, attemptId);
        if (attempt.getStatus() != AttemptStatus.in_progress) {
            throw ApiException.conflict("ATTEMPT_CLOSED", "This attempt is closed.");
        }
        if (attempt.getProctoringConsent() != null) {
            throw ApiException.conflict("CONSENT_ALREADY_RECORDED",
                    "Proctoring consent has already been recorded for this attempt.");
        }
        attempt.setProctoringConsent(given);
        attemptRepository.save(attempt);
        return given;
    }

    @Transactional
    public ProctorEventsResponse addEvents(UUID userId, UUID attemptId,
                                           List<ProctorEventRequest> requests) {
        AssessmentAttempt attempt = requireOwned(userId, attemptId);
        long existing = eventRepository.countByAttemptId(attempt.getId());
        if (attempt.getStatus() != AttemptStatus.in_progress
                || !Boolean.TRUE.equals(attempt.getProctoringConsent())
                || requests == null || requests.isEmpty()) {
            return new ProctorEventsResponse(0, existing);
        }
        long remaining = MAX_EVENTS - existing;
        int recorded = 0;
        for (ProctorEventRequest request : requests) {
            if (remaining <= 0) {
                break;
            }
            if (request == null || request.type() == null) {
                continue;
            }
            ProctorEventType type;
            try {
                type = ProctorEventType.valueOf(request.type().trim().toLowerCase());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ProctorEvent event = new ProctorEvent();
            event.setAttemptId(attempt.getId());
            event.setType(type);
            event.setOccurredAt(request.occurredAt() == null ? clock.instant() : request.occurredAt());
            event.setDurationSeconds(Math.max(0, request.durationSeconds() == null
                    ? 0 : request.durationSeconds()));
            eventRepository.save(event);
            recorded++;
            remaining--;
        }
        return new ProctorEventsResponse(recorded, existing + recorded);
    }

    @Transactional
    public String explanation(UUID userId, UUID attemptId, String text) {
        AssessmentAttempt attempt = requireOwned(userId, attemptId);
        if (!Boolean.TRUE.equals(attempt.getProctoringConsent())) {
            throw ApiException.conflict("CONSENT_REQUIRED",
                    "An explanation is only available when focus signals were recorded.");
        }
        String clean = text == null ? null : text.strip();
        if (clean != null && clean.length() > 500) {
            throw ApiException.badRequest("EXPLANATION_TOO_LONG",
                    "The explanation must be at most 500 characters.");
        }
        attempt.setExplanation(clean == null || clean.isEmpty() ? null : clean);
        attemptRepository.save(attempt);
        return attempt.getExplanation();
    }

    private AssessmentAttempt requireOwned(UUID userId, UUID attemptId) {
        AssessmentAttempt attempt = attemptRepository.findWithLockById(attemptId)
                .orElseThrow(() -> ApiException.notFound("ATTEMPT_NOT_FOUND", "Attempt not found."));
        if (!attempt.getUserId().equals(userId)) {
            throw ApiException.notFound("ATTEMPT_NOT_FOUND", "Attempt not found.");
        }
        return attempt;
    }
}

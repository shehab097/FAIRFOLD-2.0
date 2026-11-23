package com.fairfold.assessment;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Recomputes an application's assessment gate. Part 3.3 implements the real
 * rule once {@code job_required_assessments} exists; Part 3.2 only needs the
 * submit hook to compile and to have a single call site.
 */
@Service
public class GateService {

    public void recomputeForCandidateAssessment(UUID userId, UUID assessmentId) {
        // Implemented in Part 3.3 (gate truth table + shortlist enforcement).
    }
}

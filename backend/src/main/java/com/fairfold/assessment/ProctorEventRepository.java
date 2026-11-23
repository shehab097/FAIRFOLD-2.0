package com.fairfold.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProctorEventRepository extends JpaRepository<ProctorEvent, UUID> {

    List<ProctorEvent> findByAttemptIdOrderByOccurredAtAsc(UUID attemptId);

    long countByAttemptId(UUID attemptId);
}

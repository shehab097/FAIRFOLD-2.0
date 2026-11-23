package com.fairfold.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewPackRepository extends JpaRepository<InterviewPack, UUID> {

    Optional<InterviewPack> findByJobId(UUID jobId);
}

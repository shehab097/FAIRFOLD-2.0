package com.fairfold.resume;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface ResumeSuggestionRepository extends JpaRepository<ResumeSuggestion, UUID> {

    Optional<ResumeSuggestion> findByResumeId(UUID resumeId);

    @Transactional
    void deleteByResumeId(UUID resumeId);
}

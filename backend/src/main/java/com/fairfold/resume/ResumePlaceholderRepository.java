package com.fairfold.resume;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ResumePlaceholderRepository extends JpaRepository<ResumePlaceholder, UUID> {

    List<ResumePlaceholder> findByResumeIdOrderByPlaceholderAsc(UUID resumeId);

    @Transactional
    void deleteByResumeId(UUID resumeId);
}

package com.fairfold.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByUserIdAndCurrentTrue(UUID userId);

    List<Resume> findByUserIdAndCurrentTrueOrderByCreatedAtDesc(UUID userId);
}

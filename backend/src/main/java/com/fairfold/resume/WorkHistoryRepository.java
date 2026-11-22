package com.fairfold.resume;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkHistoryRepository extends JpaRepository<WorkHistory, UUID> {

    List<WorkHistory> findByUserIdOrderByStartMonthDesc(UUID userId);

    Optional<WorkHistory> findByIdAndUserId(UUID id, UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}

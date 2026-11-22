package com.fairfold.screening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ScreeningItemRepository extends JpaRepository<ScreeningItem, UUID> {

    List<ScreeningItem> findByRunIdOrderByCreatedAtAsc(UUID runId);

    List<ScreeningItem> findByRunIdAndStatusOrderByCreatedAtAsc(UUID runId, ItemStatus status);

    List<ScreeningItem> findByStatus(ItemStatus status);

    List<ScreeningItem> findByRunIdAndStatusIn(UUID runId, Collection<ItemStatus> statuses);

    long countByRunIdAndStatusIn(UUID runId, Collection<ItemStatus> statuses);

    @Transactional
    @Modifying
    @Query("update ScreeningItem i set i.status = com.fairfold.screening.ItemStatus.queued, i.error = null where i.status = com.fairfold.screening.ItemStatus.running")
    int resetRunningToQueued();

    @Transactional
    @Modifying
    @Query("update ScreeningItem i set i.status = com.fairfold.screening.ItemStatus.queued, i.error = null where i.runId = :runId and i.status = com.fairfold.screening.ItemStatus.failed")
    int requeueFailed(@Param("runId") UUID runId);
}

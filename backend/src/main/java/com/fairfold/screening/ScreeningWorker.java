package com.fairfold.screening;

import com.fairfold.ai.IdentifierLeakException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Runs one screening run sequentially on an async thread. Four threads let up
 * to four different jobs run at once; items of one run are processed one by one
 * so counters and status transitions stay consistent.
 */
@Service
public class ScreeningWorker {

    private static final Logger log = LoggerFactory.getLogger(ScreeningWorker.class);

    private final ScreeningRunRepository runRepository;
    private final ScreeningItemRepository itemRepository;
    private final ScreeningItemProcessor processor;

    public ScreeningWorker(ScreeningRunRepository runRepository,
                           ScreeningItemRepository itemRepository,
                           ScreeningItemProcessor processor) {
        this.runRepository = runRepository;
        this.itemRepository = itemRepository;
        this.processor = processor;
    }

    @Async("screeningExecutor")
    public void processRun(UUID runId) {
        ScreeningRun run = runRepository.findById(runId).orElse(null);
        if (run == null || run.getStatus() != RunStatus.running) {
            return;
        }
        List<ScreeningItem> pending = itemRepository.findByRunIdAndStatusIn(
                runId, List.of(ItemStatus.queued, ItemStatus.running));
        for (ScreeningItem item : pending) {
            try {
                processor.process(item.getId());
            } catch (IdentifierLeakException e) {
                processor.markFailed(item.getId(), "IDENTIFIER_LEAK");
            } catch (Exception e) {
                // Never log resume text or identifiers.
                log.warn("Screening item {} failed: {}", item.getId(), e.getClass().getSimpleName());
                processor.markFailed(item.getId(), "SCREENING_FAILED");
            }
        }
        long remaining = itemRepository.countByRunIdAndStatusIn(
                runId, List.of(ItemStatus.queued, ItemStatus.running));
        if (remaining == 0) {
            runRepository.finish(runId, RunStatus.finished, Instant.now());
        }
    }
}

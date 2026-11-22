package com.fairfold.screening;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Restart recovery: running items go back to queued and their runs resume. */
@Component
public class ScreeningRecovery {

    private static final Logger log = LoggerFactory.getLogger(ScreeningRecovery.class);

    private final ScreeningItemRepository itemRepository;
    private final ScreeningRunRepository runRepository;
    private final ScreeningWorker worker;

    public ScreeningRecovery(ScreeningItemRepository itemRepository,
                             ScreeningRunRepository runRepository,
                             ScreeningWorker worker) {
        this.itemRepository = itemRepository;
        this.runRepository = runRepository;
        this.worker = worker;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        int reset = itemRepository.resetRunningToQueued();
        if (reset > 0) {
            log.info("Reset {} running screening items to queued", reset);
        }
        for (ScreeningRun run : runRepository.findByStatus(RunStatus.running)) {
            worker.processRun(run.getId());
        }
    }
}

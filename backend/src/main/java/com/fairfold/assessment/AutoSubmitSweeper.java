package com.fairfold.assessment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Submits every still-open attempt whose timer has run out, using the answers
 * saved so far. Runs every minute; the attempt service applies normal scoring.
 */
@Component
public class AutoSubmitSweeper {

    private static final Logger log = LoggerFactory.getLogger(AutoSubmitSweeper.class);

    private final AttemptService attemptService;

    public AutoSubmitSweeper(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @Scheduled(fixedDelay = 60_000)
    public int sweep() {
        int submitted = attemptService.submitExpired();
        if (submitted > 0) {
            log.info("Auto-submitted {} expired assessment attempts", submitted);
        }
        return submitted;
    }
}

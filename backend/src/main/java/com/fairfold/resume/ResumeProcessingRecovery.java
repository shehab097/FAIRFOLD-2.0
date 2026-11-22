package com.fairfold.resume;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Resumes left in {@code processing} when the process stopped are put back on
 * the async queue at startup.
 */
@Component
public class ResumeProcessingRecovery {

    private static final Logger log = LoggerFactory.getLogger(ResumeProcessingRecovery.class);

    private final ResumeRepository resumeRepository;
    private final ResumeProcessingService processingService;

    public ResumeProcessingRecovery(ResumeRepository resumeRepository,
                                    ResumeProcessingService processingService) {
        this.resumeRepository = resumeRepository;
        this.processingService = processingService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requeue() {
        var pending = resumeRepository.findByStatus(ResumeStatus.processing);
        for (Resume resume : pending) {
            log.info("Requeueing resume processing for {}", resume.getId());
            processingService.process(resume.getId());
        }
    }
}

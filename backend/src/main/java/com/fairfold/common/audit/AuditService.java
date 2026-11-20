package com.fairfold.common.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Records an audit entry. Runs in a new transaction so that a failed
     * business transaction still leaves an audit trail where appropriate.
     * {@code detailsJson} must be valid JSON (or "{}").
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, String action, String resourceType, String resourceId, String detailsJson) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actorId);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setDetails(detailsJson == null || detailsJson.isBlank() ? "{}" : detailsJson);
        repository.save(entry);
    }
}

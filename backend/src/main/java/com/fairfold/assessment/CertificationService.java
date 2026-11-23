package com.fairfold.assessment;

import com.fairfold.assessment.dto.CertificationRequest;
import com.fairfold.assessment.dto.CertificationResponse;
import com.fairfold.common.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CertificationService {

    private final CertificationRepository repository;

    public CertificationService(CertificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CertificationResponse> list(UUID userId) {
        return repository.findByUserIdOrderByIssuedOnDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public CertificationResponse create(UUID userId, CertificationRequest request) {
        Certification certification = new Certification();
        certification.setUserId(userId);
        apply(certification, request);
        repository.save(certification);
        return toResponse(certification);
    }

    @Transactional
    public CertificationResponse update(UUID userId, UUID id, CertificationRequest request) {
        Certification certification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("CERTIFICATION_NOT_FOUND",
                        "Certification not found."));
        apply(certification, request);
        repository.save(certification);
        return toResponse(certification);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Certification certification = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("CERTIFICATION_NOT_FOUND",
                        "Certification not found."));
        repository.delete(certification);
    }

    private void apply(Certification certification, CertificationRequest request) {
        if (request.expiresOn() != null && request.issuedOn() != null
                && request.expiresOn().isBefore(request.issuedOn())) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "The expiry date cannot be before the issue date.");
        }
        certification.setName(request.name().strip());
        certification.setIssuer(request.issuer().strip());
        certification.setIssuedOn(request.issuedOn());
        certification.setExpiresOn(request.expiresOn());
        String credential = request.credentialId() == null ? null : request.credentialId().strip();
        certification.setCredentialId(credential == null || credential.isEmpty() ? null : credential);
    }

    private CertificationResponse toResponse(Certification certification) {
        return new CertificationResponse(
                certification.getId(), certification.getName(), certification.getIssuer(),
                certification.getIssuedOn(), certification.getExpiresOn(),
                certification.getCredentialId());
    }
}

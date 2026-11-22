package com.fairfold.screening;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gives each candidate one random alias per company (FR-054). The alias is not
 * derived from any personal data and is stable across jobs for that company.
 */
@Service
public class AliasService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CandidateAliasRepository repository;

    public AliasService(CandidateAliasRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String aliasFor(UUID companyId, UUID candidateId) {
        return repository.findByCompanyIdAndCandidateId(companyId, candidateId)
                .map(CandidateAlias::getAlias)
                .orElseGet(() -> create(companyId, candidateId));
    }

    private String create(UUID companyId, UUID candidateId) {
        for (int attempt = 0; attempt < 25; attempt++) {
            String alias = "C-" + randomSuffix();
            if (repository.existsByCompanyIdAndAlias(companyId, alias)) {
                continue;
            }
            CandidateAlias row = new CandidateAlias();
            row.setCompanyId(companyId);
            row.setCandidateId(candidateId);
            row.setAlias(alias);
            repository.save(row);
            return alias;
        }
        throw new IllegalStateException("Could not generate a unique candidate alias");
    }

    private String randomSuffix() {
        StringBuilder out = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            out.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return out.toString();
    }
}

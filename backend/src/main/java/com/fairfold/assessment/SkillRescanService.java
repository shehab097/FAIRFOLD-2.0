package com.fairfold.assessment;

import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillCanonicalizer;
import com.fairfold.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Refreshes the skill table: trims/collapses names and auto-merges rows whose
 * names resolve to a different canonical skill through {@code skill_aliases}.
 */
@Service
public class SkillRescanService {

    private final SkillRepository skillRepository;
    private final SkillCanonicalizer canonicalizer;
    private final SkillMergeService mergeService;
    private final AuditService auditService;

    public SkillRescanService(SkillRepository skillRepository,
                              SkillCanonicalizer canonicalizer,
                              SkillMergeService mergeService,
                              AuditService auditService) {
        this.skillRepository = skillRepository;
        this.canonicalizer = canonicalizer;
        this.mergeService = mergeService;
        this.auditService = auditService;
    }

    public record RescanResult(int scanned, int renamed, int merged, int skipped,
                               List<String> skippedSkills) {}

    @Transactional
    public RescanResult rescan(UUID actorId) {
        List<Skill> skills = skillRepository.findAll();
        int renamed = 0;
        int merged = 0;
        int skipped = 0;
        List<String> skippedSkills = new ArrayList<>();

        for (Skill skill : skills) {
            Skill current = skillRepository.findById(skill.getId()).orElse(null);
            if (current == null) {
                continue; // already merged into another skill
            }
            String normalized = current.getName().strip().replaceAll("\\s+", " ");
            if (!normalized.equals(current.getName())) {
                Skill sameName = skillRepository.findByNameIgnoreCase(normalized).orElse(null);
                if (sameName != null && !sameName.getId().equals(current.getId())) {
                    try {
                        mergeService.merge(current.getId(), sameName.getId(), actorId);
                        merged++;
                    } catch (ApiException e) {
                        skipped++;
                        skippedSkills.add(current.getName());
                    }
                    continue;
                }
                current.setName(normalized);
                skillRepository.save(current);
                renamed++;
            }

            Skill canonical = canonicalizer.resolve(normalized).orElse(null);
            if (canonical != null && !canonical.getId().equals(current.getId())) {
                try {
                    mergeService.merge(current.getId(), canonical.getId(), actorId);
                    merged++;
                } catch (ApiException e) {
                    skipped++;
                    skippedSkills.add(current.getName());
                }
            }
        }
        auditService.record(actorId, "skills_rescanned", "skill", null,
                "{\"scanned\":" + skills.size() + ",\"renamed\":" + renamed
                        + ",\"merged\":" + merged + ",\"skipped\":" + skipped + "}");
        return new RescanResult(skills.size(), renamed, merged, skipped, skippedSkills);
    }
}

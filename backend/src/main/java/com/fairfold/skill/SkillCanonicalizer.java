package com.fairfold.skill;

import com.fairfold.common.errors.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Maps a written skill name (canonical, alias or free text) to one canonical
 * skills row, so JS and JavaScript never become two skills (FR-015).
 */
@Service
public class SkillCanonicalizer {

    private static final Pattern NEW_SKILL_NAME = Pattern.compile("[A-Za-z0-9+#.\\- ]{1,40}");

    private final SkillRepository skillRepository;
    private final SkillAliasRepository aliasRepository;

    public SkillCanonicalizer(SkillRepository skillRepository, SkillAliasRepository aliasRepository) {
        this.skillRepository = skillRepository;
        this.aliasRepository = aliasRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Skill> resolve(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String name = rawName.strip();
        Optional<Skill> exact = skillRepository.findByNameIgnoreCase(name);
        if (exact.isPresent()) {
            return exact;
        }
        return aliasRepository.findByAliasIgnoreCase(name)
                .flatMap(alias -> skillRepository.findById(alias.getSkillId()));
    }

    @Transactional
    public Skill resolveOrCreate(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw ApiException.badRequest("INVALID_SKILL_NAME", "Skill name is empty.");
        }
        String name = rawName.strip().replaceAll("\\s+", " ");
        Optional<Skill> existing = resolve(name);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!NEW_SKILL_NAME.matcher(name).matches()) {
            throw ApiException.badRequest("INVALID_SKILL_NAME",
                    "A new skill may use letters, digits, spaces, + # . - only (at most 40 characters).");
        }
        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public List<Skill> suggest(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.strip();
        Map<UUID, Skill> found = new LinkedHashMap<>();
        for (Skill skill : skillRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(q)) {
            found.putIfAbsent(skill.getId(), skill);
        }
        for (SkillAlias alias : aliasRepository.findTop10ByAliasContainingIgnoreCaseOrderByAliasAsc(q)) {
            skillRepository.findById(alias.getSkillId())
                    .ifPresent(skill -> found.putIfAbsent(skill.getId(), skill));
        }
        return found.values().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .limit(10)
                .toList();
    }

    public String normalise(String rawName) {
        return rawName == null ? null : rawName.strip().toLowerCase(Locale.ROOT);
    }
}

package com.fairfold.skill;

import com.fairfold.skill.dto.SkillDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {

    private final SkillCanonicalizer canonicalizer;

    public SkillService(SkillCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    @Transactional(readOnly = true)
    public List<SkillDto> suggest(String query) {
        return canonicalizer.suggest(query).stream()
                .map(SkillDto::from)
                .toList();
    }
}

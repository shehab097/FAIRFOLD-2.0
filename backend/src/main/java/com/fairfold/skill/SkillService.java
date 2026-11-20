package com.fairfold.skill;

import com.fairfold.skill.dto.SkillDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillDto> suggest(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return skillRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(query.trim())
                .stream()
                .map(SkillDto::from)
                .toList();
    }
}

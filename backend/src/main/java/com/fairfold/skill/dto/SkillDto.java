package com.fairfold.skill.dto;

import com.fairfold.skill.Skill;

import java.util.UUID;

public record SkillDto(UUID id, String name) {

    public static SkillDto from(Skill skill) {
        return new SkillDto(skill.getId(), skill.getName());
    }
}

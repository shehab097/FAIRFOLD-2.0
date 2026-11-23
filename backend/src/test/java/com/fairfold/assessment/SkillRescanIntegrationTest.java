package com.fairfold.assessment;

import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillAlias;
import com.fairfold.skill.SkillAliasRepository;
import com.fairfold.skill.SkillRepository;
import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Admin skill rescan cleans alias-shaped duplicate rows. */
class SkillRescanIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillAliasRepository aliasRepository;

    @Autowired
    private SkillRescanService rescanService;

    private String aliasName;

    @AfterEach
    void cleanup() {
        if (aliasName != null) {
            aliasRepository.findByAliasIgnoreCase(aliasName).ifPresent(aliasRepository::delete);
        }
    }

    @Test
    void rescan_merges_a_skill_row_whose_name_is_an_alias_of_the_canonical_skill() {
        UUID reactId = skillId("React");
        aliasName = "React Rescan " + UUID.randomUUID().toString().substring(0, 6);

        SkillAlias alias = new SkillAlias();
        alias.setSkillId(reactId);
        alias.setAlias(aliasName);
        aliasRepository.save(alias);

        Skill duplicate = new Skill();
        duplicate.setName(aliasName);
        duplicate = skillRepository.save(duplicate);
        UUID duplicateId = duplicate.getId();

        SkillRescanService.RescanResult result = rescanService.rescan(UUID.randomUUID());

        assertThat(result.merged()).isGreaterThanOrEqualTo(1);
        assertThat(skillRepository.findById(duplicateId)).isEmpty();
        assertThat(skillRepository.findById(reactId)).isPresent();
    }
}

package com.fairfold.skill;

import com.fairfold.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Skill normalization and alias resolution. */
class SkillCanonicalizerTest extends AbstractIntegrationTest {

    @Autowired
    private SkillCanonicalizer canonicalizer;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    void punctuation_and_alias_variants_resolve_to_one_canonical_skill() {
        Skill react = skillRepository.findByNameIgnoreCase("React").orElseThrow();
        long before = skillRepository.count();

        assertThat(canonicalizer.resolve("React.js")).get().extracting(Skill::getId)
                .isEqualTo(react.getId());
        assertThat(canonicalizer.resolve("ReactJS")).get().extracting(Skill::getId)
                .isEqualTo(react.getId());
        assertThat(canonicalizer.resolve("  react   js ")).get().extracting(Skill::getId)
                .isEqualTo(react.getId());
        assertThat(canonicalizer.resolveOrCreate("ReactJS").getId()).isEqualTo(react.getId());
        assertThat(skillRepository.count()).isEqualTo(before);
    }
}

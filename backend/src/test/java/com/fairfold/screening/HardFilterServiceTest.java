package com.fairfold.screening;

import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateSkill;
import com.fairfold.job.ExperienceLevel;
import com.fairfold.job.Job;
import com.fairfold.job.JobSkill;
import com.fairfold.job.JobSkillKind;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillAlias;
import com.fairfold.skill.SkillAliasRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HardFilterServiceTest {

    private final SkillAliasRepository aliasRepository = mock(SkillAliasRepository.class);
    private final HardFilterService service = new HardFilterService(aliasRepository);

    @Test
    void unknown_profile_data_passes() {
        Job job = job();
        job.setHfMinYears(5);
        job.setHfCountry("Bangladesh");
        CandidateProfile profile = new CandidateProfile();
        assertThat(service.evaluate(job, profile, List.of(), "", false)).isEmpty();
        assertThat(service.evaluate(job, null, null, null, false)).isEmpty();
    }

    @Test
    void known_minimum_years_contradiction_fails() {
        Job job = job();
        job.setHfMinYears(5);
        CandidateProfile profile = new CandidateProfile();
        profile.setTotalExperienceYears(new BigDecimal("3.5"));
        assertThat(service.evaluate(job, profile, List.of(), "", false))
                .contains("Minimum experience: 5 years");
    }

    @Test
    void known_country_contradiction_fails() {
        Job job = job();
        job.setHfCountry("Bangladesh");
        CandidateProfile profile = new CandidateProfile();
        profile.setLocation("Berlin, Germany");
        assertThat(service.evaluate(job, profile, List.of(), "", false))
                .contains("Country: Bangladesh");
    }

    @Test
    void must_have_skill_fails_when_absent_from_profile_and_text() {
        Skill java = skill("Java");
        Job job = job();
        job.getSkills().add(mustHave(job, java));
        when(aliasRepository.findBySkillId(java.getId())).thenReturn(List.of());

        CandidateProfile profile = new CandidateProfile();
        profile.setLocation("Dhaka, Bangladesh");
        assertThat(service.evaluate(job, profile, List.of(), "Built services with Python.", false))
                .contains("Must-have skill: Java");
    }

    @Test
    void must_have_skill_passes_from_profile_or_text_or_alias() {
        Skill java = skill("Java");
        Skill js = skill("JavaScript");
        Job job = job();
        job.getSkills().add(mustHave(job, java));
        job.getSkills().add(mustHave(job, js));
        when(aliasRepository.findBySkillId(java.getId())).thenReturn(List.of());
        SkillAlias alias = new SkillAlias();
        alias.setSkillId(js.getId());
        alias.setAlias("JS");
        when(aliasRepository.findBySkillId(js.getId())).thenReturn(List.of(alias));

        CandidateSkill profileJava = new CandidateSkill();
        profileJava.setSkillId(java.getId());

        assertThat(service.evaluate(job, new CandidateProfile(), List.of(profileJava), "JS developer", false))
                .isEmpty();
    }

    @Test
    void skip_filters_always_passes() {
        Job job = job();
        job.setHfMinYears(10);
        CandidateProfile profile = new CandidateProfile();
        profile.setTotalExperienceYears(BigDecimal.ZERO);
        assertThat(service.evaluate(job, profile, List.of(), "", true)).isEmpty();
    }

    private Job job() {
        Job job = new Job();
        job.setTitle("Backend Engineer");
        job.setDescription("Build services");
        job.setCountry("Bangladesh");
        job.setExperienceLevel(ExperienceLevel.senior);
        return job;
    }

    private JobSkill mustHave(Job job, Skill skill) {
        JobSkill js = new JobSkill();
        js.setJob(job);
        js.setSkill(skill);
        js.setKind(JobSkillKind.required);
        js.setMustHave(true);
        return js;
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName(name);
        return skill;
    }
}

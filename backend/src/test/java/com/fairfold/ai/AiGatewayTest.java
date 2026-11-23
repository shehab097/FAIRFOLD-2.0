package com.fairfold.ai;

import com.fairfold.common.audit.AuditService;
import com.fairfold.config.AppProperties;
import com.fairfold.resume.ResumeStripper;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillAliasRepository;
import com.fairfold.skill.SkillRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGatewayTest {

    private final ResumeStripper.Identifiers ids =
            new ResumeStripper.Identifiers("Tanvir", "Ahmed", "tanvir@example.com", "+8801712345678");

    @Test
    void Fr013_uses_deepseek_and_records_ok_call() {
        AiProvider deepseek = provider("deepseek", true,
                new AiResult<>(ProfileSuggestions.empty(), "deepseek-chat", 100, 50));
        AiProvider fallback = provider("rule_based", true,
                AiResult.ruleBased(ProfileSuggestions.empty()));
        AiCallRepository callRepository = mock(AiCallRepository.class);
        AuditService audit = mock(AuditService.class);
        AiGateway gateway = gateway(List.of(deepseek, fallback), callRepository, audit);

        AiGateway.ProfileExtraction result = gateway.extractProfile(
                UUID.randomUUID(), UUID.randomUUID(), "Java and Spring Boot developer.", ids);

        assertThat(result.provider()).isEqualTo("deepseek");
        ArgumentCaptor<AiCall> captor = ArgumentCaptor.forClass(AiCall.class);
        verify(callRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ok");
        assertThat(captor.getValue().getCost()).isEqualByComparingTo(new BigDecimal("0.000082"));
    }

    @Test
    void Fr013_falls_back_to_rules_when_ai_throws() {
        AiProvider deepseek = mock(AiProvider.class);
        when(deepseek.name()).thenReturn("deepseek");
        when(deepseek.available()).thenReturn(true);
        when(deepseek.extractProfile(anyString())).thenThrow(new AiProviderException("down", null));
        AiProvider fallback = provider("rule_based", true,
                AiResult.ruleBased(ProfileSuggestions.empty()));
        AiCallRepository callRepository = mock(AiCallRepository.class);
        AiGateway gateway = gateway(List.of(deepseek, fallback), callRepository, mock(AuditService.class));

        AiGateway.ProfileExtraction result = gateway.extractProfile(
                UUID.randomUUID(), UUID.randomUUID(), "Java developer.", ids);

        assertThat(result.provider()).isEqualTo("rule_based");
        ArgumentCaptor<AiCall> captor = ArgumentCaptor.forClass(AiCall.class);
        verify(callRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(AiCall::getStatus)
                .containsExactly("error", "ok");
    }

    @Test
    void Fr011_final_scan_blocks_identifier_leak_and_audits() {
        AiCallRepository callRepository = mock(AiCallRepository.class);
        AuditService audit = mock(AuditService.class);
        AiProvider deepseek = provider("deepseek", true,
                AiResult.ruleBased(ProfileSuggestions.empty()));
        AiGateway gateway = gateway(List.of(deepseek, provider("rule_based", true,
                AiResult.ruleBased(ProfileSuggestions.empty()))), callRepository, audit);
        UUID resumeId = UUID.randomUUID();

        assertThatThrownBy(() -> gateway.extractProfile(
                UUID.randomUUID(), resumeId, "My name is Tanvir Ahmed.", ids))
                .isInstanceOf(IdentifierLeakException.class);

        ArgumentCaptor<AiCall> captor = ArgumentCaptor.forClass(AiCall.class);
        verify(callRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("blocked");
        verify(audit).record(any(), org.mockito.ArgumentMatchers.eq("ai_call_blocked"),
                org.mockito.ArgumentMatchers.eq("resume"), org.mockito.ArgumentMatchers.eq(resumeId.toString()),
                anyString());
    }

    @Test
    void Fr013_rule_based_provider_extracts_dictionary_skills_and_education() {
        SkillRepository skills = mock(SkillRepository.class);
        SkillAliasRepository aliases = mock(SkillAliasRepository.class);
        when(aliases.findAll()).thenReturn(List.of());
        when(skills.findAll()).thenReturn(List.of(
                skill("Java"), skill("React"), skill("C++"), skill("SQL")));
        RuleBasedFallbackProvider provider = new RuleBasedFallbackProvider(skills, aliases);

        ProfileSuggestions result = provider.extractProfile(
                "I know Java (5 years) and React. C++ too. BSc in CSE. (3 yrs 6 mos) experience.").value();

        assertThat(result.skills()).extracting(ProfileSuggestions.SuggestedSkill::name)
                .contains("Java", "React", "C++");
        assertThat(result.skills()).filteredOn(s -> s.name().equals("Java"))
                .first().extracting(ProfileSuggestions.SuggestedSkill::years)
                .isEqualTo(new BigDecimal("5"));
        assertThat(result.educationLevel()).isEqualTo("bachelor");
        assertThat(result.totalExperienceYears()).isEqualByComparingTo(new BigDecimal("3.5"));
    }

    private AiProvider provider(String name, boolean available, AiResult<ProfileSuggestions> result) {
        AiProvider provider = mock(AiProvider.class);
        when(provider.name()).thenReturn(name);
        when(provider.available()).thenReturn(available);
        when(provider.extractProfile(anyString())).thenReturn(result);
        return provider;
    }

    private AiGateway gateway(List<AiProvider> providers, AiCallRepository repository, AuditService audit) {
        AppProperties properties = new AppProperties(
                "data", "http://localhost", false,
                new AppProperties.Auth(false),
                new AppProperties.Jwt("x", 15),
                new AppProperties.Mail("console"),
                new AppProperties.Admin("", ""),
                new AppProperties.Ai("deepseek", "key", "https://api.deepseek.com", "deepseek-chat",
                        new BigDecimal("0.27"), new BigDecimal("1.10"), 60, 1000),
                new AppProperties.Resources("0 0 3 * * SUN"),
                new AppProperties.Assessment(48),
                new AppProperties.Invite(7));
        return new AiGateway(providers, properties, new AiCallRecorder(repository, properties), audit);
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName(name);
        return skill;
    }
}

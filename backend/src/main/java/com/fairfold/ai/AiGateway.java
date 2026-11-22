package com.fairfold.ai;

import com.fairfold.common.audit.AuditService;
import com.fairfold.config.AppProperties;
import com.fairfold.resume.IdentifierScan;
import com.fairfold.resume.ResumeStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * The only entry point to AI providers. It selects the configured provider,
 * falls back to rules when AI is unavailable, records every call in
 * {@code ai_calls} (never the prompt or response), and blocks calls when a
 * candidate identifier is still present.
 */
@Service
public class AiGateway {

    private static final Logger log = LoggerFactory.getLogger(AiGateway.class);

    private final List<AiProvider> providers;
    private final AppProperties.Ai config;
    private final AiCallRepository aiCallRepository;
    private final AuditService auditService;

    public AiGateway(List<AiProvider> providers, AppProperties properties,
                     AiCallRepository aiCallRepository, AuditService auditService) {
        this.providers = providers;
        this.config = properties.ai();
        this.aiCallRepository = aiCallRepository;
        this.auditService = auditService;
    }

    public record ProfileExtraction(ProfileSuggestions suggestions, String provider) {}

    public ProfileExtraction extractProfile(UUID userId, UUID resumeId, String strippedText,
                                            ResumeStripper.Identifiers identifiers) {
        if (!strippedText.isBlank() && IdentifierScan.leaks(strippedText, identifiers)) {
            record(userId, null, "profile_extraction", null, 0, 0, "blocked");
            auditService.record(userId, "ai_call_blocked", "resume",
                    resumeId == null ? null : resumeId.toString(),
                    "{\"reason\":\"identifier_leak\",\"purpose\":\"profile_extraction\"}");
            throw new IdentifierLeakException("Personal details were still present after stripping.");
        }

        AiProvider primary = primaryProvider();
        try {
            AiResult<ProfileSuggestions> result = primary.extractProfile(strippedText);
            record(userId, null, "profile_extraction", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return new ProfileExtraction(result.value(), primary.name());
        } catch (RuntimeException e) {
            log.warn("AI profile extraction failed via {}: {}", primary.name(), e.getMessage());
            record(userId, null, "profile_extraction", primary.name(), 0, 0, "error");
            if ("rule_based".equals(primary.name())) {
                throw e;
            }
            AiProvider fallback = ruleBasedProvider();
            AiResult<ProfileSuggestions> result = fallback.extractProfile(strippedText);
            record(userId, null, "profile_extraction", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return new ProfileExtraction(result.value(), fallback.name());
        }
    }

    public AiResult<List<String>> suggestQuestions(UUID userId, AiResult.JobText job) {
        AiProvider primary = primaryProvider();
        try {
            AiResult<List<String>> result = primary.suggestQuestions(job);
            record(userId, null, "screening_questions", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return result;
        } catch (RuntimeException e) {
            record(userId, null, "screening_questions", primary.name(), 0, 0, "error");
            if ("rule_based".equals(primary.name())) {
                throw e;
            }
            return ruleBasedProvider().suggestQuestions(job);
        }
    }

    public String activeProviderName() {
        return primaryProvider().name();
    }

    private AiProvider primaryProvider() {
        if (config.deepSeekConfigured()) {
            for (AiProvider provider : providers) {
                if ("deepseek".equals(provider.name()) && provider.available()) {
                    return provider;
                }
            }
        }
        return ruleBasedProvider();
    }

    private AiProvider ruleBasedProvider() {
        return providers.stream()
                .filter(p -> "rule_based".equals(p.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No AI provider available"));
    }

    private void record(UUID userId, UUID applicationId, String purpose, String model,
                        int promptTokens, int completionTokens, String status) {
        AiCall call = new AiCall();
        call.setPurpose(purpose);
        call.setUserId(userId);
        call.setApplicationId(applicationId);
        call.setModel(model);
        call.setPromptTokens(Math.max(0, promptTokens));
        call.setCompletionTokens(Math.max(0, completionTokens));
        call.setCost(cost(promptTokens, completionTokens));
        call.setStatus(status);
        aiCallRepository.save(call);
    }

    private BigDecimal cost(int promptTokens, int completionTokens) {
        BigDecimal input = config.inputPrice()
                .multiply(BigDecimal.valueOf(Math.max(0, promptTokens)));
        BigDecimal output = config.outputPrice()
                .multiply(BigDecimal.valueOf(Math.max(0, completionTokens)));
        return input.add(output)
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}

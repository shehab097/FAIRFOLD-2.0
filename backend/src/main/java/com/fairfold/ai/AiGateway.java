package com.fairfold.ai;

import com.fairfold.common.audit.AuditService;
import com.fairfold.config.AppProperties;
import com.fairfold.resume.IdentifierScan;
import com.fairfold.resume.ResumeStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final AiCallRecorder aiCallRecorder;
    private final AuditService auditService;

    public AiGateway(List<AiProvider> providers, AppProperties properties,
                     AiCallRecorder aiCallRecorder, AuditService auditService) {
        this.providers = providers;
        this.config = properties.ai();
        this.aiCallRecorder = aiCallRecorder;
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

    public record ScreeningOutcome(List<AiResult.Verdict> verdicts, String provider) {}

    public ScreeningOutcome screen(UUID userId, UUID applicationId, UUID resumeId,
                                   AiResult.ScreeningRequest request,
                                   ResumeStripper.Identifiers identifiers) {
        if (!request.strippedText().isBlank() && IdentifierScan.leaks(request.strippedText(), identifiers)) {
            record(userId, applicationId, "screening", null, 0, 0, "blocked");
            auditService.record(userId, "ai_call_blocked", "application",
                    applicationId == null ? null : applicationId.toString(),
                    "{\"reason\":\"identifier_leak\",\"purpose\":\"screening\",\"resumeId\":\""
                            + (resumeId == null ? "" : resumeId.toString()) + "\"}");
            throw new IdentifierLeakException("Personal details were still present after stripping.");
        }
        AiProvider primary = primaryProvider();
        try {
            AiResult<List<AiResult.Verdict>> result = primary.screen(request);
            record(userId, applicationId, "screening", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return new ScreeningOutcome(result.value(), primary.name());
        } catch (RuntimeException e) {
            log.warn("AI screening failed via {}: {}", primary.name(), e.getMessage());
            record(userId, applicationId, "screening", primary.name(), 0, 0, "error");
            if ("rule_based".equals(primary.name())) {
                throw e;
            }
            AiProvider fallback = ruleBasedProvider();
            AiResult<List<AiResult.Verdict>> result = fallback.screen(request);
            record(userId, applicationId, "screening", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return new ScreeningOutcome(result.value(), fallback.name());
        }
    }

    public String activeMode() {
        return "rule_based".equals(primaryProvider().name()) ? "rule_based" : "ai";
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

    public AiResult<AiResult.InterviewPack> generateInterviewPack(
            UUID userId, AiResult.JobText job, List<String> screeningQuestions) {
        AiProvider primary = primaryProvider();
        try {
            AiResult<AiResult.InterviewPack> result =
                    primary.generateInterviewPack(job, screeningQuestions);
            record(userId, null, "interview_pack", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return result;
        } catch (RuntimeException e) {
            log.warn("AI interview pack failed via {}: {}", primary.name(), e.getMessage());
            record(userId, null, "interview_pack", primary.name(), 0, 0, "error");
            if ("rule_based".equals(primary.name())) {
                throw e;
            }
            AiResult<AiResult.InterviewPack> fallback =
                    ruleBasedProvider().generateInterviewPack(job, screeningQuestions);
            record(userId, null, "interview_pack", fallback.model(),
                    fallback.promptTokens(), fallback.completionTokens(), "ok");
            return fallback;
        }
    }

    /** No fallback: coaching feedback is unavailable when the AI provider is. */
    public AiResult<StarFeedback> gradeStar(UUID userId, String question, String anonymisedAnswer) {
        AiProvider primary = primaryProvider();
        try {
            AiResult<StarFeedback> result = primary.gradeStar(question, anonymisedAnswer);
            record(userId, null, "practice_feedback", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return result;
        } catch (RuntimeException e) {
            log.warn("AI STAR grading failed via {}: {}", primary.name(), e.getMessage());
            record(userId, null, "practice_feedback", primary.name(), 0, 0, "error");
            throw e;
        }
    }

    public AiResult<List<AiResult.GeneratedQuestion>> generateAssessmentQuestions(
            UUID userId, String skillName, int count) {
        AiProvider primary = primaryProvider();
        try {
            AiResult<List<AiResult.GeneratedQuestion>> result =
                    primary.generateAssessmentQuestions(skillName, count);
            record(userId, null, "assessment_generation", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return result;
        } catch (RuntimeException e) {
            log.warn("AI assessment generation failed via {}: {}", primary.name(), e.getMessage());
            record(userId, null, "assessment_generation", primary.name(), 0, 0, "error");
            if ("rule_based".equals(primary.name())) {
                throw e;
            }
            AiResult<List<AiResult.GeneratedQuestion>> fallback =
                    ruleBasedProvider().generateAssessmentQuestions(skillName, count);
            record(userId, null, "assessment_generation", fallback.model(),
                    fallback.promptTokens(), fallback.completionTokens(), "ok");
            return fallback;
        }
    }

    public record StoryOutcome(String story, String provider) {}

    /** No fallback: story writing is unavailable when the AI provider is. */
    public StoryOutcome writeStory(UUID userId, UUID applicationId, String strippedResume,
                                   AiResult.JobText job, ResumeStripper.Identifiers identifiers) {
        if (!strippedResume.isBlank() && IdentifierScan.leaks(strippedResume, identifiers)) {
            record(userId, applicationId, "story", null, 0, 0, "blocked");
            auditService.record(userId, "ai_call_blocked", "application",
                    applicationId == null ? null : applicationId.toString(),
                    "{\"reason\":\"identifier_leak\",\"purpose\":\"story\"}");
            throw new IdentifierLeakException("Personal details were still present after stripping.");
        }
        AiProvider primary = primaryProvider();
        try {
            AiResult<AiResult.Story> result = primary.writeStory(strippedResume, job);
            record(userId, applicationId, "story", result.model(),
                    result.promptTokens(), result.completionTokens(), "ok");
            return new StoryOutcome(result.value().story(),
                    "rule_based".equals(result.model()) ? "rule_based" : "ai");
        } catch (RuntimeException e) {
            log.warn("AI story failed via {}: {}", primary.name(), e.getMessage());
            record(userId, applicationId, "story", primary.name(), 0, 0, "error");
            throw e;
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
        aiCallRecorder.record(userId, applicationId, purpose, model,
                promptTokens, completionTokens, status);
    }
}

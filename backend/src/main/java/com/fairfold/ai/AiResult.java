package com.fairfold.ai;

import java.util.List;

public record AiResult<T>(T value, String model, int promptTokens, int completionTokens) {

    public static <T> AiResult<T> ruleBased(T value) {
        return new AiResult<>(value, "rule_based", 0, 0);
    }

    /** A requirement row sent to the screening provider. */
    public record Requirement(String key, String text, String kind) {}

    /** The request for one screening call. */
    public record ScreeningRequest(List<Requirement> requirements, String strippedText) {}

    /** One verdict returned by the provider. */
    public record Verdict(String key, String verdict, String quote) {}

    /** Job text used to suggest screening questions. */
    public record JobText(String title, String description, String requirements, List<String> skills) {}

    /** One rubric row generated for an interview pack. */
    public record RubricItem(String criterion, String description) {}

    /** Structured interview pack generated from a job. */
    public record InterviewPack(List<String> questions, List<RubricItem> rubric) {}

    /** One generated multiple-choice question for an assessment pool. */
    public record GeneratedQuestion(String text, List<GeneratedOption> options,
                                    String correctOptionId) {}

    public record GeneratedOption(String id, String text) {}

    /** A short job-specific story written from the stripped resume. */
    public record Story(String story) {}
}

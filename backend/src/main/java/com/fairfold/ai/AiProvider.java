package com.fairfold.ai;

import java.util.List;

public interface AiProvider {

    /** Provider id used in {@code resume_suggestions.provider} and audit logs. */
    String name();

    boolean available();

    AiResult<ProfileSuggestions> extractProfile(String strippedText);

    AiResult<List<AiResult.Verdict>> screen(AiResult.ScreeningRequest request);

    AiResult<List<String>> suggestQuestions(AiResult.JobText job);

    AiResult<AiResult.InterviewPack> generateInterviewPack(AiResult.JobText job,
                                                           List<String> screeningQuestions);

    AiResult<StarFeedback> gradeStar(String question, String anonymisedAnswer);

    AiResult<List<AiResult.GeneratedQuestion>> generateAssessmentQuestions(String skillName, int count);

    AiResult<AiResult.Story> writeStory(String strippedResume, AiResult.JobText job);
}

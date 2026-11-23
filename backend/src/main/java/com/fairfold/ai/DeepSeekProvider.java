package com.fairfold.ai;

import com.fairfold.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek chat-completions provider. It is OpenAI-compatible: JSON body,
 * bearer auth, and a JSON object response.
 */
@Component
public class DeepSeekProvider implements AiProvider {

    private final AppProperties.Ai config;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DeepSeekProvider(AppProperties properties, ObjectMapper objectMapper) {
        this.config = properties.ai();
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(1, config.timeoutSeconds()) * 1000;
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean available() {
        return config.deepSeekConfigured();
    }

    @Override
    public AiResult<ProfileSuggestions> extractProfile(String strippedText) {
        JsonNode root = chat("profile_extraction_system.txt", strippedText);
        ProfileSuggestions parsed = parseProfile(content(root));
        return result(root, parsed);
    }

    @Override
    public AiResult<List<AiResult.Verdict>> screen(AiResult.ScreeningRequest request) {
        String requirements = toJson(request.requirements());
        String user = "Requirements:\n" + requirements + "\n\nResume:\n" + request.strippedText();
        JsonNode root = chat("screening_system.txt", user);
        List<AiResult.Verdict> verdicts = new ArrayList<>();
        JsonNode array = readJson(content(root)).path("verdicts");
        if (array.isArray()) {
            for (JsonNode node : array) {
                verdicts.add(new AiResult.Verdict(
                        node.path("key").asText(null),
                        node.path("verdict").asText(null),
                        node.path("quote").isNull() ? null : node.path("quote").asText(null)));
            }
        }
        return result(root, verdicts);
    }

    @Override
    public AiResult<List<String>> suggestQuestions(AiResult.JobText job) {
        String user = "Job title: " + nullSafe(job.title())
                + "\nDescription: " + nullSafe(job.description())
                + "\nRequirements: " + nullSafe(job.requirements())
                + "\nSkills: " + String.join(", ", job.skills());
        JsonNode root = chat("screening_questions_system.txt", user);
        List<String> questions = new ArrayList<>();
        for (JsonNode node : readJson(content(root)).path("questions")) {
            if (node.isTextual() && !node.asText().isBlank()) {
                questions.add(node.asText().strip());
            }
        }
        return result(root, questions);
    }

    @Override
    public AiResult<AiResult.InterviewPack> generateInterviewPack(
            AiResult.JobText job, List<String> screeningQuestions) {
        String user = "Job title: " + nullSafe(job.title())
                + "\nDescription: " + nullSafe(job.description())
                + "\nRequirements: " + nullSafe(job.requirements())
                + "\nSkills: " + String.join(", ", job.skills())
                + "\nExisting screening questions:\n- "
                + String.join("\n- ", screeningQuestions == null ? List.of() : screeningQuestions);
        JsonNode root = chat("interview_pack_system.txt", user);
        JsonNode json = readJson(content(root));
        List<String> questions = new ArrayList<>();
        for (JsonNode node : json.path("questions")) {
            if (node.isTextual() && !node.asText().isBlank()) {
                questions.add(node.asText().strip());
            }
        }
        List<AiResult.RubricItem> rubric = new ArrayList<>();
        for (JsonNode node : json.path("rubric")) {
            String criterion = node.path("criterion").asText("").strip();
            if (criterion.isEmpty()) {
                continue;
            }
            rubric.add(new AiResult.RubricItem(criterion,
                    node.path("description").asText("").strip()));
        }
        return result(root, new AiResult.InterviewPack(questions, rubric));
    }

    @Override
    public AiResult<StarFeedback> gradeStar(String question, String anonymisedAnswer) {
        String user = "Question:\n" + nullSafe(question) + "\n\nAnswer:\n"
                + nullSafe(anonymisedAnswer);
        JsonNode root = chat("star-v1.txt", user);
        JsonNode json = readJson(content(root));
        StarFeedback feedback = new StarFeedback(
                component(json, "situation"),
                component(json, "task"),
                component(json, "action"),
                component(json, "result"),
                json.path("summary").asText(""));
        return result(root, feedback);
    }

    @Override
    public AiResult<List<AiResult.GeneratedQuestion>> generateAssessmentQuestions(
            String skillName, int count) {
        String user = "Skill: " + nullSafe(skillName) + "\nGenerate exactly " + count
                + " multiple-choice questions.";
        JsonNode root = chat("assessment_questions_system.txt", user);
        List<AiResult.GeneratedQuestion> questions = new ArrayList<>();
        for (JsonNode node : readJson(content(root)).path("questions")) {
            String text = node.path("text").asText("").strip();
            if (text.isEmpty()) {
                continue;
            }
            List<AiResult.GeneratedOption> options = new ArrayList<>();
            for (JsonNode option : node.path("options")) {
                String id = option.path("id").asText("").strip();
                String optionText = option.path("text").asText("").strip();
                if (!id.isEmpty() && !optionText.isEmpty()) {
                    options.add(new AiResult.GeneratedOption(id, optionText));
                }
            }
            String correct = node.path("correctOptionId").asText("").strip();
            if (options.size() >= 2 && options.stream().anyMatch(o -> o.id().equals(correct))) {
                questions.add(new AiResult.GeneratedQuestion(text, options, correct));
            }
        }
        return result(root, questions);
    }

    private StarFeedback.Component component(JsonNode root, String name) {
        JsonNode node = root.path(name);
        List<StarFeedback.Point> points = new ArrayList<>();
        for (JsonNode point : node.path("points")) {
            String comment = point.path("comment").asText("");
            String quote = point.path("quote").asText("");
            if (!comment.isBlank() || !quote.isBlank()) {
                points.add(new StarFeedback.Point(comment, quote));
            }
        }
        return new StarFeedback.Component(node.path("rating").asInt(1), points);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private JsonNode chat(String promptFile, String userMessage) {
        String system = prompt(promptFile);
        Map<String, Object> body = Map.of(
                "model", config.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userMessage)),
                "temperature", 0,
                "thinking", Map.of("type", "disabled"),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", config.maxTokens());

        RestClientException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + config.apiKey())
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientException e) {
                last = e;
                if (!retryable(e) || attempt == 1) {
                    break;
                }
            }
        }
        throw new AiProviderException("DeepSeek request failed", last);
    }

    private boolean retryable(RestClientException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        return message.contains("timed out")
                || message.contains("timeout")
                || message.contains("500")
                || message.contains("502")
                || message.contains("503")
                || message.contains("504");
    }

    private String prompt(String file) {
        try (var stream = getClass().getResourceAsStream("/prompts/" + file)) {
            if (stream == null) {
                throw new AiProviderException("Missing prompt: " + file, null);
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AiProviderException("Could not read prompt: " + file, e);
        }
    }

    private String content(JsonNode root) {
        String finish = root.path("choices").path(0).path("finish_reason").asText("");
        if ("length".equals(finish)) {
            throw new AiProviderException("DeepSeek response was cut off at the token limit", null);
        }
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual()) {
            throw new AiProviderException("DeepSeek returned no content", null);
        }
        String text = content.asText().strip();
        if (text.isEmpty()) {
            throw new AiProviderException("DeepSeek returned empty content", null);
        }
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return text;
    }

    private JsonNode readJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new AiProviderException("DeepSeek returned invalid JSON", e);
        }
    }

    private ProfileSuggestions parseProfile(String content) {
        try {
            ProfileSuggestions parsed = objectMapper.readValue(content, ProfileSuggestions.class);
            List<ProfileSuggestions.SuggestedSkill> skills = parsed.skills().stream()
                    .filter(s -> s != null && s.name() != null && !s.name().isBlank())
                    .map(s -> new ProfileSuggestions.SuggestedSkill(
                            s.name().strip(),
                            normalizeLevel(s.level()),
                            s.years() == null ? null : s.years().max(BigDecimal.ZERO)))
                    .toList();
            List<String> titles = parsed.jobTitles().stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::strip)
                    .toList();
            List<ProfileSuggestions.SuggestedWork> work = parsed.workHistory().stream()
                    .filter(w -> w != null && w.title() != null && !w.title().isBlank())
                    .map(w -> new ProfileSuggestions.SuggestedWork(
                            w.title().strip(), w.employerPlaceholder(), w.skillsUsed()))
                    .toList();
            return new ProfileSuggestions(skills, parsed.totalExperienceYears(),
                    normalizeEducation(parsed.educationLevel()), titles, work);
        } catch (Exception e) {
            throw new AiProviderException("Could not parse profile suggestions", e);
        }
    }

    private String normalizeLevel(String level) {
        if (level == null) {
            return "beginner";
        }
        String lower = level.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (lower) {
            case "expert", "advanced" -> "expert";
            case "intermediate", "medium" -> "intermediate";
            default -> "beginner";
        };
    }

    private String normalizeEducation(String level) {
        if (level == null) {
            return null;
        }
        String lower = level.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (lower) {
            case "high_school", "diploma", "bachelor", "master", "phd", "other" -> lower;
            default -> "other";
        };
    }

    private <T> AiResult<T> result(JsonNode root, T value) {
        int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
        int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
        return new AiResult<>(value, config.model(), promptTokens, completionTokens);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}

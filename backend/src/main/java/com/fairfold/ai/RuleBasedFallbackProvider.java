package com.fairfold.ai;

import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillAlias;
import com.fairfold.skill.SkillAliasRepository;
import com.fairfold.skill.SkillRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dictionary-based provider used when AI is unavailable. It never invents data:
 * skills come from the skill table and aliases, education from keywords, and
 * experience from explicit "(N yrs M mos)" durations.
 */
@Component
public class RuleBasedFallbackProvider implements AiProvider {

    private static final Pattern DURATION = Pattern.compile(
            "(?i)\\((\\d{1,2})\\s*(?:yrs?|years?)\\s+(\\d{1,2})\\s*(?:mos?|months?)\\)");

    private final SkillRepository skillRepository;
    private final SkillAliasRepository aliasRepository;

    public RuleBasedFallbackProvider(SkillRepository skillRepository, SkillAliasRepository aliasRepository) {
        this.skillRepository = skillRepository;
        this.aliasRepository = aliasRepository;
    }

    @Override
    public String name() {
        return "rule_based";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public AiResult<ProfileSuggestions> extractProfile(String strippedText) {
        String text = strippedText == null ? "" : strippedText;
        Map<UUID, ProfileSuggestions.SuggestedSkill> found = new LinkedHashMap<>();
        List<Skill> allSkills = skillRepository.findAll();
        Map<UUID, String> names = new LinkedHashMap<>();
        for (Skill skill : allSkills) {
            names.put(skill.getId(), skill.getName());
            addIfPresent(text, found, skill.getName(), skill.getId(), skill.getName(), null);
        }
        for (SkillAlias alias : aliasRepository.findAll()) {
            addIfPresent(text, found, alias.getAlias(), alias.getSkillId(),
                    names.getOrDefault(alias.getSkillId(), alias.getAlias()), null);
        }

        BigDecimal total = durationSum(text);
        List<ProfileSuggestions.SuggestedSkill> skills = new ArrayList<>(found.values());
        return AiResult.ruleBased(new ProfileSuggestions(
                skills,
                total,
                educationLevel(text),
                List.of(),
                List.of()));
    }

    private void addIfPresent(String text, Map<UUID, ProfileSuggestions.SuggestedSkill> found,
                              String term, UUID skillId, String displayName, BigDecimal years) {
        if (term == null || term.isBlank()) {
            return;
        }
        Pattern pattern = Pattern.compile("(?i)(?<![\\w+#])" + Pattern.quote(term) + "(?![\\w+#])");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return;
        }
        BigDecimal detectedYears = years != null ? years : yearsNear(text, matcher.end());
        String level = detectedYears != null
                ? (detectedYears.compareTo(BigDecimal.valueOf(5)) >= 0 ? "expert"
                : detectedYears.compareTo(BigDecimal.valueOf(2)) >= 0 ? "intermediate" : "beginner")
                : "intermediate";
        ProfileSuggestions.SuggestedSkill existing = found.get(skillId);
        if (existing == null || existing.years() == null) {
            found.put(skillId, new ProfileSuggestions.SuggestedSkill(displayName, level, detectedYears));
        }
    }

    private BigDecimal yearsNear(String text, int end) {
        String window = text.substring(end, Math.min(text.length(), end + 40));
        Matcher matcher = Pattern.compile("(?i)\\(?(\\d{1,2})(?:\\.\\d+)?\\s*(?:\\+\\s*)?(?:years?|yrs?)\\)?")
                .matcher(window);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
    }

    private BigDecimal durationSum(String text) {
        double years = 0;
        Matcher matcher = DURATION.matcher(text);
        while (matcher.find()) {
            years += Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                years += Integer.parseInt(matcher.group(2)) / 12.0;
            }
        }
        if (years == 0) {
            return null;
        }
        return BigDecimal.valueOf(Math.round(years * 10) / 10.0);
    }

    private String educationLevel(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (matchesWord(lower, "phd") || matchesWord(lower, "doctorate")) {
            return "phd";
        }
        if (matchesWord(lower, "master") || matchesWord(lower, "msc") || matchesWord(lower, "m.sc")
                || matchesWord(lower, "m.s") || matchesWord(lower, "mba")
                || matchesWord(lower, "m.eng") || matchesWord(lower, "m.tech")) {
            return "master";
        }
        if (matchesWord(lower, "bachelor") || matchesWord(lower, "bsc") || matchesWord(lower, "b.sc")
                || matchesWord(lower, "b.s") || matchesWord(lower, "bba") || matchesWord(lower, "ba")
                || matchesWord(lower, "b.eng") || matchesWord(lower, "b.tech")) {
            return "bachelor";
        }
        if (matchesWord(lower, "diploma")) {
            return "diploma";
        }
        if (matchesWord(lower, "hsc") || matchesWord(lower, "ssc") || lower.contains("high school")) {
            return "high_school";
        }
        return null;
    }

    private boolean matchesWord(String lower, String word) {
        return Pattern.compile("(?<![a-z])" + Pattern.quote(word) + "(?![a-z])").matcher(lower).find();
    }

    @Override
    public AiResult<List<AiResult.Verdict>> screen(AiResult.ScreeningRequest request) {
        List<AiResult.Verdict> verdicts = new ArrayList<>();
        String text = request.strippedText() == null ? "" : request.strippedText();
        for (AiResult.Requirement requirement : request.requirements()) {
            if (requirement.key() != null && requirement.key().startsWith("skill:")) {
                verdicts.add(screenSkill(requirement, text));
            } else if ("experience".equals(requirement.key())) {
                verdicts.add(screenExperience(requirement));
            } else {
                verdicts.add(new AiResult.Verdict(requirement.key(), "not_met", null));
            }
        }
        return AiResult.ruleBased(verdicts);
    }

    private AiResult.Verdict screenSkill(AiResult.Requirement requirement, String text) {
        UUID skillId;
        try {
            skillId = UUID.fromString(requirement.key().substring("skill:".length()));
        } catch (IllegalArgumentException e) {
            return new AiResult.Verdict(requirement.key(), "not_met", null);
        }
        Skill skill = skillRepository.findById(skillId).orElse(null);
        if (skill == null) {
            return new AiResult.Verdict(requirement.key(), "not_met", null);
        }
        List<String> terms = new ArrayList<>();
        terms.add(skill.getName());
        for (SkillAlias alias : aliasRepository.findBySkillId(skillId)) {
            terms.add(alias.getAlias());
        }
        for (String term : terms) {
            Matcher matcher = Pattern.compile("(?i)(?<![\\w+#])" + Pattern.quote(term) + "(?![\\w+#])")
                    .matcher(text);
            if (matcher.find()) {
                return new AiResult.Verdict(requirement.key(), "met",
                        sentenceAround(text, matcher.start(), matcher.end()));
            }
        }
        return new AiResult.Verdict(requirement.key(), "not_met", null);
    }

    private AiResult.Verdict screenExperience(AiResult.Requirement requirement) {
        String text = requirement.text() == null ? "" : requirement.text();
        Matcher levelMatcher = Pattern.compile("(?i)Experience level:\\s*(entry|mid|senior|lead)").matcher(text);
        String level = levelMatcher.find() ? levelMatcher.group(1).toLowerCase(Locale.ROOT) : "entry";
        Matcher yearsMatcher = Pattern.compile("(?i)profile:\\s*(\\d+(?:\\.\\d+)?)\\s*years?").matcher(text);
        BigDecimal years = yearsMatcher.find() ? new BigDecimal(yearsMatcher.group(1)) : null;
        int threshold = switch (level) {
            case "mid" -> 2;
            case "senior" -> 5;
            case "lead" -> 8;
            default -> 0;
        };
        if (years == null) {
            return new AiResult.Verdict(requirement.key(), threshold == 0 ? "met" : "not_met", null);
        }
        double value = years.doubleValue();
        if (value >= threshold) {
            return new AiResult.Verdict(requirement.key(), "met", null);
        }
        if (value > 0 && value >= threshold / 2.0) {
            return new AiResult.Verdict(requirement.key(), "partial", null);
        }
        return new AiResult.Verdict(requirement.key(), "not_met", null);
    }

    private String sentenceAround(String text, int start, int end) {
        int from = start;
        while (from > 0 && !isBoundary(text.charAt(from - 1))) {
            from--;
        }
        int to = end;
        while (to < text.length() && !isBoundary(text.charAt(to))) {
            to++;
        }
        if (to < text.length()) {
            to++;
        }
        String sentence = text.substring(from, to).strip();
        if (sentence.length() <= 250) {
            return sentence;
        }
        int center = Math.max(0, (start + end) / 2 - from - 125);
        int cut = Math.min(sentence.length() - 250, center);
        return sentence.substring(Math.max(0, cut), Math.min(sentence.length(), Math.max(0, cut) + 250)).strip();
    }

    private boolean isBoundary(char value) {
        return value == '.' || value == '!' || value == '?' || value == '\n';
    }

    @Override
    public AiResult<List<String>> suggestQuestions(AiResult.JobText job) {
        List<String> questions = new ArrayList<>();
        for (String skill : job.skills()) {
            if (questions.size() >= 6) {
                break;
            }
            if (skill != null && !skill.isBlank()) {
                questions.add("Tell us about your experience with " + skill.strip() + ".");
            }
        }
        List<String> fallback = List.of(
                "Walk us through a recent project you are proud of.",
                "How do you handle competing deadlines?",
                "Describe a time you had to learn something quickly.");
        for (String question : fallback) {
            if (questions.size() >= 6) {
                break;
            }
            questions.add(question);
        }
        return AiResult.ruleBased(questions);
    }

    @Override
    public AiResult<AiResult.InterviewPack> generateInterviewPack(
            AiResult.JobText job, List<String> screeningQuestions) {
        List<String> questions = new ArrayList<>();
        for (String skill : job.skills()) {
            if (questions.size() >= 8) {
                break;
            }
            if (skill != null && !skill.isBlank()) {
                questions.add("Tell us about your experience with " + skill.strip()
                        + " and a problem you solved with it.");
            }
        }
        List<String> generic = List.of(
                "Describe a challenging project you finished.",
                "How do you handle disagreement in a team?",
                "Why are you interested in this role?",
                "Walk us through a recent technical decision you made.",
                "How do you keep your skills current?");
        for (String question : generic) {
            if (questions.size() >= 6) {
                break;
            }
            questions.add(question);
        }
        while (questions.size() < 6) {
            questions.add("Tell us about a result you are proud of in this kind of role.");
        }
        List<AiResult.RubricItem> rubric = List.of(
                new AiResult.RubricItem("Technical skills",
                        "Depth and accuracy of job-relevant technical knowledge."),
                new AiResult.RubricItem("Problem solving",
                        "How the candidate breaks down problems and weighs trade-offs."),
                new AiResult.RubricItem("Communication",
                        "Clear, structured answers and attentive listening."),
                new AiResult.RubricItem("Role fit",
                        "Motivation and evidence of working at this level and scope."));
        return AiResult.ruleBased(new AiResult.InterviewPack(questions, rubric));
    }

    @Override
    public AiResult<StarFeedback> gradeStar(String question, String anonymisedAnswer) {
        throw new AiProviderException("STAR grading requires an AI provider", null);
    }
}

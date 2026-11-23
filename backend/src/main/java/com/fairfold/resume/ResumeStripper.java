package com.fairfold.resume;

import java.time.LocalDate;
import java.util.List;

/**
 * Removes personal details from extracted resume text (BR-05). Pure and
 * deterministic: all matching terms are passed in, nothing is loaded from the
 * database here.
 */
public final class ResumeStripper {

    private static final String NAME = "[NAME]";
    private static final String EMAIL = "[EMAIL]";
    private static final String PHONE = "[PHONE]";
    private static final String URL = "[URL]";
    private static final String YEAR = "[YEAR]";
    private static final String DATE = "[DATE]";

    public record Identifiers(String firstName, String lastName, String email, String phone) {
        public String fullName() {
            String first = firstName == null ? "" : firstName.trim();
            String last = lastName == null ? "" : lastName.trim();
            String full = (first + " " + last).trim();
            return full.length() >= 3 ? full : null;
        }

        public static Identifiers none() {
            return new Identifiers(null, null, null, null);
        }
    }

    public record Terms(List<String> institutions, List<String> employers) {
        public static Terms empty() {
            return new Terms(List.of(), List.of());
        }
    }

    public record Mapping(
            String placeholder,
            String kind,
            String originalValue,
            LocalDate startMonth,
            LocalDate endMonth,
            boolean current
    ) {}

    public record Result(String text, List<Mapping> mappings) {}

    public Result strip(String extractedText, Identifiers identifiers, Terms terms) {
        if (extractedText == null || extractedText.isBlank()) {
            return new Result("", List.of());
        }
        String text = extractedText.replace("\r\n", "\n").replace('\r', '\n');

        text = stripKnownIdentifiers(text, identifiers);
        text = stripHeaderName(text);
        text = removePersonalDetailLines(text);
        text = stripLinkAndContactPatterns(text);

        Registry registry = new Registry();
        text = stripNamedTerms(text, terms, registry);
        text = stripInstitutionAndEmployerPatterns(text, registry);

        text = stripEducationDates(text);
        text = stripWorkDates(text, registry);

        return new Result(text.strip(), registry.mappings());
    }

    /**
     * Practice-mode anonymisation: known identifiers, contact patterns,
     * institutions and employers only. Header-name, personal-line and date
     * removal steps are intentionally skipped.
     */
    public Result anonymise(String text, Identifiers identifiers, Terms terms) {
        if (text == null || text.isBlank()) {
            return new Result("", List.of());
        }
        String clean = text.replace("\r\n", "\n").replace('\r', '\n');
        clean = stripKnownIdentifiers(clean, identifiers);
        clean = stripLinkAndContactPatterns(clean);

        Registry registry = new Registry();
        clean = stripNamedTerms(clean, terms, registry);
        clean = stripInstitutionAndEmployerPatterns(clean, registry);

        return new Result(clean.strip(), registry.mappings());
    }

    // --- step 1 ------------------------------------------------------------

    private String stripKnownIdentifiers(String text, Identifiers ids) {
        if (ids == null) {
            return text;
        }
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        addIfUsable(names, ids.fullName());
        addToken(names, ids.firstName());
        addToken(names, ids.lastName());
        if (ids.email() != null && !ids.email().isBlank()) {
            text = text.replaceAll("(?i)" + java.util.regex.Pattern.quote(ids.email().trim()), EMAIL);
        }
        if (ids.phone() != null && !ids.phone().isBlank()) {
            text = text.replaceAll(java.util.regex.Pattern.quote(ids.phone().trim()), PHONE);
        }
        if (!names.isEmpty()) {
            String alternatives = names.stream()
                    .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                    .map(java.util.regex.Pattern::quote)
                    .collect(java.util.stream.Collectors.joining("|"));
            text = text.replaceAll("(?i)(?<![A-Za-z])(" + alternatives + ")(?![A-Za-z])", NAME);
        }
        return text;
    }

    private void addIfUsable(java.util.Set<String> names, String value) {
        if (value != null && value.trim().length() >= 3 && value.chars().anyMatch(Character::isLetter)) {
            names.add(value.trim());
        }
    }

    private void addToken(java.util.Set<String> names, String value) {
        if (value == null) {
            return;
        }
        for (String token : value.trim().split("\\s+")) {
            if (token.length() >= 3 && token.chars().anyMatch(Character::isLetter)) {
                names.add(token);
            }
        }
    }

    // --- step 2 ------------------------------------------------------------

    private String stripHeaderName(String text) {
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isHeaderName(trimmed)) {
                return text.replaceFirst(java.util.regex.Pattern.quote(line), NAME);
            }
            return text;
        }
        return text;
    }

    private boolean isHeaderName(String line) {
        if (line.matches(".*\\d.*")) {
            return false;
        }
        String[] words = line.split("\\s+");
        if (words.length < 2 || words.length > 4) {
            return false;
        }
        for (String word : words) {
            if (!word.matches("[A-Z][A-Za-z'.-]*")) {
                return false;
            }
        }
        return true;
    }

    // --- step 3 ------------------------------------------------------------

    private static final java.util.regex.Pattern PERSONAL_LINE = java.util.regex.Pattern.compile(
            "^(?:Date of Birth|DOB|Born|Age|Gender|Sex|Marital Status|Nationality|Religion|"
                    + "Father|Mother|Spouse|NID|National ID|Passport|Present Address|Permanent Address|"
                    + "Address|Blood Group|Photo)\\b.*$",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private String removePersonalDetailLines(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (PERSONAL_LINE.matcher(line.strip()).matches()) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString().replaceAll("\n{3,}", "\n\n");
    }

    // --- step 4 ------------------------------------------------------------

    private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile(
            "(?:https?://\\S+|www\\.\\S+|(?:linkedin|github)\\.com/\\S+)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final java.util.regex.Pattern PHONE_PATTERN = java.util.regex.Pattern.compile(
            "\\+?\\(?\\d[\\d ()\\.\\-]{5,}\\d");

    private static final java.util.regex.Pattern YEAR_RANGE = java.util.regex.Pattern.compile(
            "^\\d{4}\\s*[-–—]\\s*\\d{4}$");

    private String stripLinkAndContactPatterns(String text) {
        text = replaceAll(text, URL_PATTERN, URL);
        text = replaceAll(text, EMAIL_PATTERN, EMAIL);
        return replaceAllWithCheck(text, PHONE_PATTERN, match ->
                isYearRange(match) || digitCount(match) < 7 ? match : PHONE);
    }

    private boolean isYearRange(String value) {
        String core = value.replaceAll("[()\\s]", "");
        return YEAR_RANGE.matcher(core).matches();
    }

    private int digitCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private String replaceAll(String text, java.util.regex.Pattern pattern, String replacement) {
        return replaceAllWithCheck(text, pattern, match -> replacement);
    }

    private String replaceAllWithCheck(String text, java.util.regex.Pattern pattern,
                                       java.util.function.Function<String, String> replacement) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(
                    replacement.apply(matcher.group())));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    // --- step 5 ------------------------------------------------------------

    private record MappingBuilder(String placeholder, String kind, String originalValue,
                                  LocalDate startMonth, LocalDate endMonth, boolean current) {
        MappingBuilder withDates(LocalDate start, LocalDate end, boolean current) {
            return new MappingBuilder(placeholder, kind, originalValue, start, end, current);
        }

        Mapping toMapping() {
            return new Mapping(placeholder, kind, originalValue, startMonth, endMonth, current);
        }
    }

    private static final class Registry {
        private final java.util.Map<String, java.util.Map<String, MappingBuilder>> byKind = new java.util.HashMap<>();
        private final java.util.Map<String, Integer> counters = new java.util.HashMap<>();

        String placeholder(String kind, String original) {
            var existingByOriginal = byKind.computeIfAbsent(kind, k -> new java.util.LinkedHashMap<>());
            String key = original.strip().toLowerCase(java.util.Locale.ROOT);
            MappingBuilder existing = existingByOriginal.get(key);
            if (existing != null) {
                return "[" + existing.placeholder() + "]";
            }
            int next = counters.merge(kind, 1, Integer::sum);
            String placeholder = kind.toUpperCase(java.util.Locale.ROOT) + "_" + next;
            existingByOriginal.put(key, new MappingBuilder(placeholder, kind, original.strip(),
                    null, null, false));
            return "[" + placeholder + "]";
        }

        void attachDates(String placeholder, LocalDate start, LocalDate end, boolean current) {
            for (var map : byKind.values()) {
                for (var entry : map.entrySet()) {
                    if (entry.getValue().placeholder().equals(placeholder)) {
                        entry.setValue(entry.getValue().withDates(start, end, current));
                        return;
                    }
                }
            }
        }

        List<Mapping> mappings() {
            return byKind.values().stream()
                    .flatMap(map -> map.values().stream())
                    .map(MappingBuilder::toMapping)
                    .toList();
        }
    }

    private String stripNamedTerms(String text, Terms terms, Registry registry) {
        if (terms != null) {
            text = stripTermList(text, terms.institutions(), "institution", registry);
            text = stripTermList(text, terms.employers(), "employer", registry);
        }
        return text;
    }

    private String stripTermList(String text, List<String> terms, String kind, Registry registry) {
        if (terms == null || terms.isEmpty()) {
            return text;
        }
        List<String> sorted = terms.stream()
                .filter(t -> t != null && t.strip().length() >= 3)
                .map(String::strip)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
        for (String term : sorted) {
            String needle = term.toLowerCase(java.util.Locale.ROOT);
            if (!text.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                continue;
            }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?i)(?<![A-Za-z0-9])" + java.util.regex.Pattern.quote(term) + "(?![A-Za-z0-9])");
            text = replaceAllWithCheck(text, pattern, match -> registry.placeholder(kind, match));
        }
        return text;
    }

    private static final java.util.regex.Pattern INSTITUTION_PATTERN = java.util.regex.Pattern.compile(
            "(?:[A-Z][\\w&.'-]*(?: [A-Z][\\w&.'-]*)* )?"
                    + "(?:University|College|Institute|School|Academy)"
                    + "(?: of [A-Z][\\w ]*)?");

    private static final java.util.regex.Pattern EMPLOYER_PATTERN = java.util.regex.Pattern.compile(
            "[A-Z][\\w&.'-]*(?: [A-Z][\\w&.'-]*)*,? "
                    + "(?:Ltd\\.?|Limited|Inc\\.?|LLC|Corp\\.?|Co\\.|Company|PLC|Technologies|Solutions|Systems|Bank|Group)");

    private String stripInstitutionAndEmployerPatterns(String text, Registry registry) {
        text = replaceAllWithCheck(text, INSTITUTION_PATTERN,
                match -> registry.placeholder("institution", match));
        return replaceAllWithCheck(text, EMPLOYER_PATTERN,
                match -> registry.placeholder("employer", match));
    }

    // --- step 6 ------------------------------------------------------------

    private static final java.util.regex.Pattern DEGREE_WORD = java.util.regex.Pattern.compile(
            "\\b(BSc|B\\.Sc|MSc|M\\.Sc|Bachelor|Master|PhD|HSC|SSC|Diploma|GPA|CGPA)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern EDUCATION_RANGE = java.util.regex.Pattern.compile(
            "\\b(?:19|20)\\d{2}\\s*[-–—]\\s*(?:(?:19|20)\\d{2}|Present|Current)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern FOUR_DIGIT_YEAR = java.util.regex.Pattern.compile(
            "\\b(?:19|20)\\d{2}\\b");

    private String stripEducationDates(String text) {
        String[] lines = text.split("\n", -1);
        boolean[] education = new boolean[lines.length];
        for (int i = 0; i < lines.length; i++) {
            education[i] = lines[i].contains("[INSTITUTION_") || DEGREE_WORD.matcher(lines[i]).find();
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean context = education[i]
                    || (isYearLine(line) && ((i > 0 && education[i - 1])
                    || (i + 1 < lines.length && education[i + 1])));
            if (context) {
                line = EDUCATION_RANGE.matcher(line).replaceAll(YEAR);
                line = FOUR_DIGIT_YEAR.matcher(line).replaceAll(YEAR);
            }
            if (i > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    private boolean isYearLine(String line) {
        return FOUR_DIGIT_YEAR.matcher(line).find()
                && line.strip().matches("(?i)[\\d\\s\\-–—presentcurrent,()]+");
    }

    private static final java.util.regex.Pattern MONTH_NAME = java.util.regex.Pattern.compile(
            "(?i)(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|"
                    + "Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)");

    private static final java.util.regex.Pattern WORK_DATE = java.util.regex.Pattern.compile(
            "(?i)((?:(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|"
                    + "Dec(?:ember)?)\\s+)?(?:19|20)\\d{2})"
                    + "\\s*(?:-|–|—|to)\\s*"
                    + "((?:(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|"
                    + "Dec(?:ember)?)\\s+)?(?:19|20)\\d{2}|Present|Current)");

    private static final java.util.regex.Pattern EMPLOYER_PLACEHOLDER = java.util.regex.Pattern.compile(
            "\\[(EMPLOYER_\\d+)]");

    private String stripWorkDates(String text, Registry registry) {
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            java.util.regex.Matcher matcher = WORK_DATE.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            java.util.regex.Matcher employer = EMPLOYER_PLACEHOLDER.matcher(line);
            String placeholder = employer.find() ? employer.group(1) : nearestEmployer(lines, i);
            if (placeholder == null) {
                continue;
            }
            StringBuilder replaced = new StringBuilder();
            matcher.reset();
            while (matcher.find()) {
                DateRange range = parseRange(matcher.group(1), matcher.group(2));
                boolean alreadyWrapped = matcher.start() > 0 && line.charAt(matcher.start() - 1) == '('
                        && matcher.end() < line.length() && line.charAt(matcher.end()) == ')';
                String replacement;
                if (range == null) {
                    replacement = DATE;
                } else {
                    replacement = alreadyWrapped ? duration(range) : "(" + duration(range) + ")";
                    registry.attachDates(placeholder, range.start(), range.end(), range.current());
                }
                matcher.appendReplacement(replaced, java.util.regex.Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(replaced);
            lines[i] = replaced.toString();
        }
        return String.join("\n", lines);
    }

    private String nearestEmployer(String[] lines, int index) {
        for (int distance = 1; distance <= 2; distance++) {
            for (int candidate : new int[]{index - distance, index + distance}) {
                if (candidate >= 0 && candidate < lines.length) {
                    java.util.regex.Matcher matcher = EMPLOYER_PLACEHOLDER.matcher(lines[candidate]);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        }
        return null;
    }

    private record DateRange(LocalDate start, LocalDate end, boolean current) {}

    private DateRange parseRange(String startText, String endText) {
        LocalDate start = parseMonth(startText);
        if (start == null) {
            return null;
        }
        boolean current = endText.equalsIgnoreCase("present") || endText.equalsIgnoreCase("current");
        LocalDate end = current ? null : parseMonth(endText);
        if (!current && end == null) {
            return null;
        }
        return new DateRange(start, end, current);
    }

    private LocalDate parseMonth(String value) {
        String clean = value.strip().replaceAll("\\s+", " ");
        java.util.regex.Matcher yearMatcher = java.util.regex.Pattern.compile("(?:19|20)\\d{2}").matcher(clean);
        if (!yearMatcher.find()) {
            return null;
        }
        int year = Integer.parseInt(yearMatcher.group());
        java.util.regex.Matcher monthMatcher = MONTH_NAME.matcher(clean);
        if (!monthMatcher.find()) {
            return LocalDate.of(year, 1, 1);
        }
        return LocalDate.of(year, monthNumber(monthMatcher.group(1)), 1);
    }

    private int monthNumber(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith("jan")) return 1;
        if (lower.startsWith("feb")) return 2;
        if (lower.startsWith("mar")) return 3;
        if (lower.startsWith("apr")) return 4;
        if (lower.startsWith("may")) return 5;
        if (lower.startsWith("jun")) return 6;
        if (lower.startsWith("jul")) return 7;
        if (lower.startsWith("aug")) return 8;
        if (lower.startsWith("sep")) return 9;
        if (lower.startsWith("oct")) return 10;
        if (lower.startsWith("nov")) return 11;
        return 12;
    }

    private String duration(DateRange range) {
        LocalDate end = range.current() ? LocalDate.now() : range.end();
        long months = java.time.temporal.ChronoUnit.MONTHS.between(range.start(), end);
        if (months < 0) {
            months = 0;
        }
        long years = months / 12;
        long remaining = months % 12;
        return years + (years == 1 ? " yr" : " yrs") + " " + remaining
                + (remaining == 1 ? " mo" : " mos");
    }
}

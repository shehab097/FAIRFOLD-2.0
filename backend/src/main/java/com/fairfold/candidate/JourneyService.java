package com.fairfold.candidate;

import com.fairfold.candidate.dto.JourneyResponse;
import com.fairfold.resume.MonthFormat;
import com.fairfold.resume.WorkHistory;
import com.fairfold.resume.WorkHistoryRepository;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillCanonicalizer;
import com.fairfold.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** FR-016/FR-017: career timeline and skill evolution from work history. */
@Service
public class JourneyService {

    private final WorkHistoryRepository workHistoryRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillCanonicalizer canonicalizer;
    private final Clock clock;

    public JourneyService(WorkHistoryRepository workHistoryRepository,
                          CandidateSkillRepository candidateSkillRepository,
                          SkillRepository skillRepository,
                          SkillCanonicalizer canonicalizer,
                          Clock clock) {
        this.workHistoryRepository = workHistoryRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.skillRepository = skillRepository;
        this.canonicalizer = canonicalizer;
        this.clock = clock;
    }

    private static final class Accumulator {
        private final UUID skillId;
        private final String name;
        private String level;
        private boolean verified;
        private LocalDate first;
        private LocalDate last;
        private final LinkedHashSet<String> roles = new LinkedHashSet<>();

        private Accumulator(UUID skillId, String name) {
            this.skillId = skillId;
            this.name = name;
        }
    }

    @Transactional(readOnly = true)
    public JourneyResponse journey(UUID userId) {
        List<WorkHistory> history = new ArrayList<>(
                workHistoryRepository.findByUserIdOrderByStartMonthDesc(userId));
        history.sort(Comparator.comparing(WorkHistory::getStartMonth,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<JourneyResponse.TimelineItem> timeline = history.stream()
                .map(item -> new JourneyResponse.TimelineItem(
                        item.getId(), item.getTitle(), item.getEmployer(),
                        MonthFormat.format(item.getStartMonth()),
                        MonthFormat.format(item.getEndMonth()), item.isCurrent()))
                .toList();

        Map<UUID, Accumulator> skills = new LinkedHashMap<>();
        for (WorkHistory item : history) {
            for (String raw : item.getSkillsUsed()) {
                Skill skill = canonicalizer.resolve(raw).orElse(null);
                if (skill == null) {
                    continue;
                }
                Accumulator accumulator = skills.computeIfAbsent(skill.getId(),
                        id -> new Accumulator(id, skill.getName()));
                if (item.getStartMonth() != null
                        && (accumulator.first == null || item.getStartMonth().isBefore(accumulator.first))) {
                    accumulator.first = item.getStartMonth();
                }
                LocalDate end = item.isCurrent()
                        ? YearMonth.now(clock).atDay(1)
                        : item.getEndMonth() != null ? item.getEndMonth() : item.getStartMonth();
                if (end != null && (accumulator.last == null || end.isAfter(accumulator.last))) {
                    accumulator.last = end;
                }
                if (item.getTitle() != null) {
                    accumulator.roles.add(item.getTitle());
                }
            }
        }

        List<CandidateSkill> profileSkills = candidateSkillRepository
                .findByUserIdOrderByCreatedAtAsc(userId);
        for (CandidateSkill profile : profileSkills) {
            Skill skill = skillRepository.findById(profile.getSkillId()).orElse(null);
            if (skill == null) {
                continue;
            }
            Accumulator accumulator = skills.computeIfAbsent(skill.getId(),
                    id -> new Accumulator(id, skill.getName()));
            accumulator.level = profile.getLevel() == null ? null : profile.getLevel().name();
            accumulator.verified = profile.isVerified();
        }

        List<JourneyResponse.SkillEvolution> skillItems = skills.values().stream()
                .sorted(Comparator
                        .comparing((Accumulator a) -> a.first,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(a -> a.name, String.CASE_INSENSITIVE_ORDER))
                .map(a -> new JourneyResponse.SkillEvolution(
                        a.skillId, a.name, a.level, a.verified,
                        MonthFormat.format(a.first), MonthFormat.format(a.last),
                        List.copyOf(a.roles)))
                .toList();

        return new JourneyResponse(timeline, skillItems);
    }
}

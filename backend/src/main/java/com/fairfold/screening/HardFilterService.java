package com.fairfold.screening;

import com.fairfold.candidate.CandidateProfile;
import com.fairfold.candidate.CandidateSkill;
import com.fairfold.job.Job;
import com.fairfold.job.JobSkill;
import com.fairfold.skill.SkillAlias;
import com.fairfold.skill.SkillAliasRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * BR-03 hard filters. Only known contradicting data fails; missing data passes.
 */
@Service
public class HardFilterService {

    private final SkillAliasRepository aliasRepository;

    public HardFilterService(SkillAliasRepository aliasRepository) {
        this.aliasRepository = aliasRepository;
    }

    public Optional<String> evaluate(Job job, CandidateProfile profile,
                                     List<CandidateSkill> candidateSkills,
                                     String strippedText, boolean skipFilters) {
        if (skipFilters) {
            return Optional.empty();
        }
        if (job.getHfMinYears() != null && profile != null
                && profile.getTotalExperienceYears() != null
                && profile.getTotalExperienceYears().doubleValue() < job.getHfMinYears()) {
            return Optional.of("Minimum experience: " + job.getHfMinYears() + " years");
        }
        if (job.getHfCountry() != null && !job.getHfCountry().isBlank()
                && profile != null && profile.getLocation() != null
                && !profile.getLocation().toLowerCase(Locale.ROOT)
                .contains(job.getHfCountry().strip().toLowerCase(Locale.ROOT))) {
            return Optional.of("Country: " + job.getHfCountry().strip());
        }

        Set<UUID> profileSkills = new HashSet<>();
        if (candidateSkills != null) {
            for (CandidateSkill skill : candidateSkills) {
                profileSkills.add(skill.getSkillId());
            }
        }
        for (JobSkill required : job.getSkills()) {
            if (!required.isMustHave()) {
                continue;
            }
            UUID skillId = required.getSkill().getId();
            String name = required.getSkill().getName();
            if (profileSkills.contains(skillId)) {
                continue;
            }
            if (appearsInText(strippedText, name)) {
                continue;
            }
            boolean inAlias = aliasRepository.findBySkillId(skillId).stream()
                    .map(SkillAlias::getAlias)
                    .anyMatch(alias -> appearsInText(strippedText, alias));
            if (!inAlias) {
                return Optional.of("Must-have skill: " + name);
            }
        }
        return Optional.empty();
    }

    private boolean appearsInText(String text, String term) {
        if (text == null || text.isBlank() || term == null || term.isBlank()) {
            return false;
        }
        Pattern pattern = Pattern.compile("(?i)(?<![\\w+#])" + Pattern.quote(term.strip()) + "(?![\\w+#])");
        return pattern.matcher(text).find();
    }
}

package com.fairfold.assessment;

import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillCanonicalizer;
import com.fairfold.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * One draft assessment topic per canonical skill. Creating a topic never
 * activates it: it waits in pending_generation until an admin generates and
 * approves the pool.
 */
@Service
public class AssessmentQueueService {

    private final AssessmentRepository assessmentRepository;
    private final SkillRepository skillRepository;
    private final SkillCanonicalizer canonicalizer;

    public AssessmentQueueService(AssessmentRepository assessmentRepository,
                                  SkillRepository skillRepository,
                                  SkillCanonicalizer canonicalizer) {
        this.assessmentRepository = assessmentRepository;
        this.skillRepository = skillRepository;
        this.canonicalizer = canonicalizer;
    }

    @Transactional
    public Assessment ensureTopicForSkill(UUID skillId) {
        if (skillId == null) {
            return null;
        }
        List<Assessment> existing = assessmentRepository.findBySkillIdOrderByCreatedAtAsc(skillId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        Skill skill = skillRepository.findById(skillId).orElse(null);
        if (skill == null) {
            return null;
        }
        Assessment assessment = new Assessment();
        assessment.setTitle(skill.getName() + " Assessment");
        assessment.setSkillId(skill.getId());
        assessment.setDurationMinutes(30);
        assessment.setPassMark(70);
        assessment.setActive(false);
        assessment.setQuestionsPerAttempt(10);
        assessment.setReviewStatus("pending_generation");
        return assessmentRepository.save(assessment);
    }

    @Transactional
    public Assessment ensureTopicForName(String rawName) {
        return canonicalizer.resolve(rawName)
                .map(skill -> ensureTopicForSkill(skill.getId()))
                .orElse(null);
    }

    @Transactional
    public void ensureTopicsForNames(Collection<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            ensureTopicForName(name);
        }
    }
}

package com.fairfold.assessment;

import com.fairfold.candidate.CandidateSkill;
import com.fairfold.candidate.CandidateSkillRepository;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.job.JobSkill;
import com.fairfold.job.JobSkillRepository;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillAlias;
import com.fairfold.skill.SkillAliasRepository;
import com.fairfold.skill.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SkillMergeService {

    private final SkillRepository skillRepository;
    private final SkillAliasRepository aliasRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final JobSkillRepository jobSkillRepository;
    private final LearningResourceRepository resourceRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AuditService auditService;

    public SkillMergeService(SkillRepository skillRepository,
                             SkillAliasRepository aliasRepository,
                             CandidateSkillRepository candidateSkillRepository,
                             JobSkillRepository jobSkillRepository,
                             LearningResourceRepository resourceRepository,
                             AssessmentRepository assessmentRepository,
                             AssessmentAttemptRepository attemptRepository,
                             AuditService auditService) {
        this.skillRepository = skillRepository;
        this.aliasRepository = aliasRepository;
        this.candidateSkillRepository = candidateSkillRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.resourceRepository = resourceRepository;
        this.assessmentRepository = assessmentRepository;
        this.attemptRepository = attemptRepository;
        this.auditService = auditService;
    }

    public record MergeResult(UUID sourceSkillId, UUID targetSkillId,
                              int movedCandidateSkills, int removedCandidateSkills,
                              int movedJobSkills, int removedJobSkills,
                              int movedResources, int removedAssessments,
                              boolean movedAssessment) {}

    @Transactional
    public MergeResult merge(UUID sourceSkillId, UUID targetSkillId, UUID actorId) {
        if (sourceSkillId == null || targetSkillId == null || sourceSkillId.equals(targetSkillId)) {
            throw ApiException.badRequest("INVALID_SKILL_MERGE",
                    "Choose two different skills to merge.");
        }
        Skill source = skillRepository.findById(sourceSkillId)
                .orElseThrow(() -> ApiException.notFound("SKILL_NOT_FOUND", "Source skill not found."));
        Skill target = skillRepository.findById(targetSkillId)
                .orElseThrow(() -> ApiException.notFound("SKILL_NOT_FOUND", "Target skill not found."));

        int movedCandidates = 0;
        int removedCandidates = 0;
        for (CandidateSkill row : candidateSkillRepository.findBySkillId(sourceSkillId)) {
            if (candidateSkillRepository.existsByUserIdAndSkillId(row.getUserId(), targetSkillId)) {
                candidateSkillRepository.delete(row);
                removedCandidates++;
            } else {
                row.setSkillId(targetSkillId);
                candidateSkillRepository.save(row);
                movedCandidates++;
            }
        }

        int movedJobs = 0;
        int removedJobs = 0;
        for (JobSkill row : jobSkillRepository.findBySkillId(sourceSkillId)) {
            UUID jobId = row.getJob().getId();
            if (jobSkillRepository.existsByJobIdAndSkillId(jobId, targetSkillId)) {
                jobSkillRepository.delete(row);
                removedJobs++;
            } else {
                row.setSkill(target);
                jobSkillRepository.save(row);
                movedJobs++;
            }
        }

        List<LearningResource> resources = resourceRepository.findBySkillId(sourceSkillId);
        for (LearningResource resource : resources) {
            resource.setSkillId(targetSkillId);
            resourceRepository.save(resource);
        }

        // Move aliases that pointed at the duplicate, then remember its name.
        for (SkillAlias alias : aliasRepository.findBySkillId(sourceSkillId)) {
            alias.setSkillId(targetSkillId);
            aliasRepository.save(alias);
        }
        if (aliasRepository.findByAliasIgnoreCase(source.getName()).isEmpty()) {
            SkillAlias alias = new SkillAlias();
            alias.setSkillId(targetSkillId);
            alias.setAlias(source.getName());
            aliasRepository.save(alias);
        }

        List<Assessment> sourceAssessments = assessmentRepository
                .findBySkillIdOrderByCreatedAtAsc(sourceSkillId);
        boolean movedAssessment = false;
        int removedAssessments = 0;
        boolean targetHasAssessment = !assessmentRepository
                .findBySkillIdOrderByCreatedAtAsc(targetSkillId).isEmpty();
        for (Assessment assessment : sourceAssessments) {
            if (attemptRepository.countByAssessmentId(assessment.getId()) > 0) {
                throw ApiException.conflict("ASSESSMENT_HAS_ATTEMPTS",
                        "Cannot merge: the source assessment already has attempts.");
            }
        }
        if (!sourceAssessments.isEmpty() && !targetHasAssessment) {
            Assessment keep = sourceAssessments.get(0);
            keep.setSkillId(targetSkillId);
            keep.setTitle(target.getName() + " Assessment");
            assessmentRepository.save(keep);
            movedAssessment = true;
            for (int i = 1; i < sourceAssessments.size(); i++) {
                assessmentRepository.delete(sourceAssessments.get(i));
                removedAssessments++;
            }
        } else {
            for (Assessment assessment : sourceAssessments) {
                assessmentRepository.delete(assessment);
                removedAssessments++;
            }
        }

        skillRepository.delete(source);
        auditService.record(actorId, "skill_merged", "skill", targetSkillId.toString(),
                "{\"sourceSkillId\":\"" + sourceSkillId + "\",\"targetSkillId\":\""
                        + targetSkillId + "\"}");
        return new MergeResult(sourceSkillId, targetSkillId,
                movedCandidates, removedCandidates, movedJobs, removedJobs,
                resources.size(), removedAssessments, movedAssessment);
    }
}

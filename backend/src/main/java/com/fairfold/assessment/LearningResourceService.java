package com.fairfold.assessment;

import com.fairfold.assessment.dto.ActiveRequest;
import com.fairfold.assessment.dto.LearningResourceDto;
import com.fairfold.assessment.dto.ResourceRequest;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LearningResourceService {

    private static final Set<String> COSTS = Set.of("free", "paid");
    private static final Set<String> LEVELS = Set.of("beginner", "intermediate", "advanced");

    private final LearningResourceRepository resourceRepository;
    private final SkillRepository skillRepository;
    private final AuditService auditService;

    public LearningResourceService(LearningResourceRepository resourceRepository,
                                   SkillRepository skillRepository,
                                   AuditService auditService) {
        this.resourceRepository = resourceRepository;
        this.skillRepository = skillRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<LearningResourceDto> list(UUID skillId, Pageable pageable) {
        Page<LearningResource> page = skillId == null
                ? resourceRepository.findAll(pageable)
                : resourceRepository.findBySkillId(skillId, pageable);
        Map<UUID, Skill> skills = skillsFor(page.getContent().stream()
                .map(LearningResource::getSkillId).toList());
        return PageResponse.of(page.getContent().stream()
                .map(r -> toDto(r, skills))
                .toList(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public LearningResourceDto create(UUID actorId, ResourceRequest request) {
        LearningResource resource = new LearningResource();
        apply(resource, request);
        resourceRepository.save(resource);
        auditService.record(actorId, "resource_created", "resource",
                resource.getId().toString(), "{}");
        return toDto(resource, Map.of());
    }

    @Transactional
    public LearningResourceDto update(UUID actorId, UUID id, ResourceRequest request) {
        LearningResource resource = require(id);
        apply(resource, request);
        resourceRepository.save(resource);
        auditService.record(actorId, "resource_updated", "resource", id.toString(), "{}");
        return toDto(resource, Map.of());
    }

    @Transactional
    public LearningResourceDto setActive(UUID actorId, UUID id, ActiveRequest request) {
        LearningResource resource = require(id);
        resource.setActive(Boolean.TRUE.equals(request.active()));
        resourceRepository.save(resource);
        auditService.record(actorId, "resource_updated", "resource", id.toString(), "{}");
        return toDto(resource, Map.of());
    }

    private LearningResource require(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("RESOURCE_NOT_FOUND", "Resource not found."));
    }

    private void apply(LearningResource resource, ResourceRequest request) {
        Skill skill = skillRepository.findById(request.skillId())
                .orElseThrow(() -> ApiException.badRequest("SKILL_NOT_FOUND", "Skill not found."));
        String cost = request.cost() == null ? "free" : request.cost().strip().toLowerCase();
        if (!COSTS.contains(cost)) {
            throw ApiException.badRequest("INVALID_COST", "Cost must be 'free' or 'paid'.");
        }
        String level = request.level() == null ? "" : request.level().strip().toLowerCase();
        if (!LEVELS.contains(level)) {
            throw ApiException.badRequest("INVALID_LEVEL",
                    "Level must be 'beginner', 'intermediate' or 'advanced'.");
        }
        String url = request.url().strip();
        if (!isHttpUrl(url)) {
            throw ApiException.badRequest("INVALID_URL", "The resource URL must start with http:// or https://.");
        }
        resource.setSkillId(skill.getId());
        resource.setTitle(request.title().strip());
        resource.setUrl(url);
        resource.setCost(cost);
        resource.setLanguage(request.language() == null || request.language().isBlank()
                ? "English" : request.language().strip());
        resource.setLevel(level);
        resource.setActive(request.active() == null || request.active());
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Map<UUID, Skill> skillsFor(List<UUID> skillIds) {
        Map<UUID, Skill> result = new LinkedHashMap<>();
        skillRepository.findAllById(new HashSet<>(skillIds)).forEach(s -> result.put(s.getId(), s));
        return result;
    }

    private LearningResourceDto toDto(LearningResource resource, Map<UUID, Skill> skills) {
        Skill skill = skills.get(resource.getSkillId());
        if (skill == null) {
            skill = skillRepository.findById(resource.getSkillId()).orElse(null);
        }
        return new LearningResourceDto(
                resource.getId(),
                resource.getSkillId(),
                skill == null ? null : skill.getName(),
                resource.getTitle(),
                resource.getUrl(),
                resource.getCost(),
                resource.getLanguage(),
                resource.getLevel(),
                resource.isActive(),
                resource.isBroken(),
                resource.getLastCheckedAt());
    }
}

package com.fairfold.job;

import com.fairfold.assessment.Assessment;
import com.fairfold.assessment.AssessmentRepository;
import com.fairfold.assessment.GateService;
import com.fairfold.assessment.JobRequiredAssessment;
import com.fairfold.assessment.JobRequiredAssessmentRepository;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.company.Company;
import com.fairfold.company.CompanyRepository;
import com.fairfold.job.dto.EmployerJobDetail;
import com.fairfold.job.dto.EmployerJobListItem;
import com.fairfold.job.dto.JobRequest;
import com.fairfold.job.dto.PublicJobDetail;
import com.fairfold.job.dto.PublicJobListItem;
import com.fairfold.job.dto.RequiredAssessmentRequest;
import com.fairfold.skill.Skill;
import com.fairfold.skill.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;
    private final AuditService auditService;
    private final JobScreeningQuestionRepository questionRepository;
    private final JobRequiredAssessmentRepository requiredAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final GateService gateService;
    private final Clock clock;

    public JobService(JobRepository jobRepository, SkillRepository skillRepository,
                      CompanyRepository companyRepository, AuditService auditService,
                      JobScreeningQuestionRepository questionRepository,
                      JobRequiredAssessmentRepository requiredAssessmentRepository,
                      AssessmentRepository assessmentRepository,
                      GateService gateService, Clock clock) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.companyRepository = companyRepository;
        this.auditService = auditService;
        this.questionRepository = questionRepository;
        this.requiredAssessmentRepository = requiredAssessmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.gateService = gateService;
        this.clock = clock;
    }

    // --- public board ------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<PublicJobListItem> searchPublic(String q, String location, String country,
                                                         UUID companyId, BigDecimal salaryMin, BigDecimal salaryMax,
                                                         Boolean remote, String level, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        String qEscaped = escapeLike(query);
        String levelParam = normalize(level);
        String locationParam = normalize(location);
        String countryParam = normalize(country);
        String companyParam = companyId == null ? null : companyId.toString();

        if (companyId != null && companyRepository.findById(companyId)
                .filter(Company::isShowNamePublicly).isEmpty()) {
            return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }

        Page<Job> page = jobRepository.searchActive(
                query, qEscaped, locationParam, countryParam, companyParam, salaryMin, salaryMax,
                remote, levelParam, pageable);
        return PageResponse.of(
                page.getContent().stream().map(this::toPublicListItem).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PublicJobDetail getPublicJob(UUID jobId) {
        Job job = jobRepository.findByIdAndStatus(jobId, JobStatus.active)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        return toPublicDetail(job);
    }

    // --- employer ----------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<EmployerJobListItem> listEmployerJobs(UUID companyId, String status, Pageable pageable) {
        JobStatus statusParam = status == null || status.isBlank() ? null : parseStatus(status);
        Page<Job> page = jobRepository.findByCompany(companyId, statusParam, pageable);
        return PageResponse.of(
                page.getContent().stream()
                        .map(job -> new EmployerJobListItem(
                                job.getId(), job.getTitle(), job.getStatus(), 0, 0, job.getCreatedAt()))
                        .toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployerJobDetail getEmployerJob(UUID companyId, UUID jobId) {
        return toEmployerDetail(loadForCompany(companyId, jobId));
    }

    @Transactional
    public EmployerJobDetail create(UUID companyId, UUID userId, JobRequest request) {
        Job job = new Job();
        job.setCompanyId(companyId);
        job.setCreatedBy(userId);
        applyRequest(job, request);
        jobRepository.save(job);
        replaceRequiredAssessments(job, request.requiredAssessments());
        auditService.record(userId, "job_created", "job", job.getId().toString(), "{}");
        return toEmployerDetail(job);
    }

    @Transactional
    public EmployerJobDetail update(UUID companyId, UUID userId, UUID jobId, JobRequest request) {
        Job job = loadForCompany(companyId, jobId);
        if (job.getStatus() != JobStatus.draft) {
            throw ApiException.conflict("JOB_NOT_DRAFT", "Only draft jobs can be edited.");
        }
        applyRequest(job, request);
        jobRepository.save(job);
        replaceRequiredAssessments(job, request.requiredAssessments());
        auditService.record(userId, "job_updated", "job", job.getId().toString(), "{}");
        return toEmployerDetail(job);
    }

    @Transactional
    public void delete(UUID companyId, UUID userId, UUID jobId) {
        Job job = loadForCompany(companyId, jobId);
        if (job.getStatus() != JobStatus.draft) {
            throw ApiException.conflict("JOB_NOT_DRAFT", "Only draft jobs can be deleted.");
        }
        jobRepository.delete(job);
        auditService.record(userId, "job_deleted", "job", jobId.toString(), "{}");
    }

    @Transactional
    public EmployerJobDetail activate(UUID companyId, UUID userId, UUID jobId) {
        Job job = loadForCompany(companyId, jobId);
        requireStatus(job, JobStatus.draft, "activate");
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ApiException.notFound("COMPANY_NOT_FOUND", "Company not found."));
        if (!isComplete(company)) {
            throw ApiException.conflict("COMPANY_INCOMPLETE", "Complete your company profile before activating a job.");
        }
        job.setStatus(JobStatus.active);
        if (job.getPublishedAt() == null) {
            job.setPublishedAt(clock.instant());
        }
        auditService.record(userId, "job_activated", "job", job.getId().toString(), "{}");
        return toEmployerDetail(job);
    }

    @Transactional
    public EmployerJobDetail pause(UUID companyId, UUID userId, UUID jobId) {
        Job job = loadForCompany(companyId, jobId);
        requireStatus(job, JobStatus.active, "pause");
        job.setStatus(JobStatus.paused);
        auditService.record(userId, "job_paused", "job", job.getId().toString(), "{}");
        return toEmployerDetail(job);
    }

    @Transactional
    public EmployerJobDetail resume(UUID companyId, UUID userId, UUID jobId) {
        Job job = loadForCompany(companyId, jobId);
        requireStatus(job, JobStatus.paused, "resume");
        job.setStatus(JobStatus.active);
        auditService.record(userId, "job_resumed", "job", job.getId().toString(), "{}");
        return toEmployerDetail(job);
    }

    @Transactional
    public EmployerJobDetail close(UUID companyId, UUID userId, UUID jobId) {
        Job job = loadForCompany(companyId, jobId);
        if (job.getStatus() != JobStatus.active && job.getStatus() != JobStatus.paused) {
            throw ApiException.conflict("JOB_ILLEGAL_TRANSITION",
                    "Only active or paused jobs can be closed.");
        }
        job.setStatus(JobStatus.closed);
        job.setClosedAt(clock.instant());
        auditService.record(userId, "job_closed", "job", job.getId().toString(), "{}");
        return toEmployerDetail(job);
    }

    @Transactional
    public EmployerJobDetail duplicate(UUID companyId, UUID userId, UUID jobId) {
        Job source = loadForCompany(companyId, jobId);
        Job copy = new Job();
        copy.setCompanyId(companyId);
        copy.setCreatedBy(userId);
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setRequirements(source.getRequirements());
        copy.setExperienceLevel(source.getExperienceLevel());
        copy.setCity(source.getCity());
        copy.setAddress(source.getAddress());
        copy.setCountry(source.getCountry());
        copy.setRemote(source.isRemote());
        copy.setSalaryType(source.getSalaryType());
        copy.setSalaryMin(source.getSalaryMin());
        copy.setSalaryMax(source.getSalaryMax());
        copy.setSalaryCurrency(source.getSalaryCurrency());
        copy.setSalaryPeriod(source.getSalaryPeriod());
        copy.setSalaryNegotiable(source.isSalaryNegotiable());
        copy.setHfMinYears(source.getHfMinYears());
        copy.setHfCountry(source.getHfCountry());
        copy.setCutoffScore(source.getCutoffScore());
        copy.setStatus(JobStatus.draft);

        List<JobSkill> copiedSkills = new ArrayList<>();
        for (JobSkill js : source.getSkills()) {
            JobSkill copySkill = new JobSkill();
            copySkill.setJob(copy);
            copySkill.setSkill(js.getSkill());
            copySkill.setKind(js.getKind());
            copySkill.setMustHave(js.isMustHave());
            copiedSkills.add(copySkill);
        }
        copy.setSkills(copiedSkills);

        jobRepository.save(copy);
        for (JobRequiredAssessment sourceRequirement
                : requiredAssessmentRepository.findByJobId(source.getId())) {
            JobRequiredAssessment requirement = new JobRequiredAssessment();
            requirement.setJobId(copy.getId());
            requirement.setAssessmentId(sourceRequirement.getAssessmentId());
            requirement.setMinScore(sourceRequirement.getMinScore());
            requiredAssessmentRepository.save(requirement);
        }
        int position = 0;
        for (JobScreeningQuestion sourceQuestion
                : questionRepository.findByJobIdOrderByPositionAsc(source.getId())) {
            JobScreeningQuestion question = new JobScreeningQuestion();
            question.setJobId(copy.getId());
            question.setPosition(position++);
            question.setText(sourceQuestion.getText());
            questionRepository.save(question);
        }
        auditService.record(userId, "job_duplicated", "job", copy.getId().toString(), "{}");
        gateService.recomputeForJob(copy.getId());
        return toEmployerDetail(copy);
    }

    // --- helpers -----------------------------------------------------------

    private Job loadForCompany(UUID companyId, UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
    }

    private void requireStatus(Job job, JobStatus expected, String action) {
        if (job.getStatus() != expected) {
            throw ApiException.conflict("JOB_ILLEGAL_TRANSITION",
                    "A job in status '" + job.getStatus().name() + "' cannot be " + action + "d.");
        }
    }

    private void replaceRequiredAssessments(Job job, List<RequiredAssessmentRequest> requests) {
        List<RequiredAssessmentRequest> items = requests == null ? List.of() : requests;
        Set<UUID> seen = new HashSet<>();
        for (RequiredAssessmentRequest item : items) {
            if (item == null || item.assessmentId() == null || item.minScore() == null) {
                throw ApiException.badRequest("INVALID_ASSESSMENT_REQUIREMENT",
                        "Every required assessment needs an assessment and a minimum score.");
            }
            if (!seen.add(item.assessmentId())) {
                throw ApiException.badRequest("DUPLICATE_ASSESSMENT",
                        "The same assessment cannot be required twice.");
            }
            Assessment assessment = assessmentRepository.findById(item.assessmentId())
                    .orElseThrow(() -> ApiException.badRequest("ASSESSMENT_NOT_FOUND",
                            "One or more assessments do not exist."));
            if (!assessment.isActive()) {
                throw ApiException.badRequest("ASSESSMENT_INACTIVE",
                        "Only active assessments can be required.");
            }
            if (item.minScore() < 1 || item.minScore() > 100) {
                throw ApiException.badRequest("INVALID_ASSESSMENT_REQUIREMENT",
                        "The minimum score must be between 1 and 100.");
            }
        }
        requiredAssessmentRepository.deleteByJobId(job.getId());
        for (RequiredAssessmentRequest item : items) {
            JobRequiredAssessment requirement = new JobRequiredAssessment();
            requirement.setJobId(job.getId());
            requirement.setAssessmentId(item.assessmentId());
            requirement.setMinScore(item.minScore());
            requiredAssessmentRepository.save(requirement);
        }
    }

    private void applyRequest(Job job, JobRequest request) {
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setRequirements(request.requirements() == null ? "" : request.requirements());
        job.setExperienceLevel(parseLevel(request.experienceLevel()));
        job.setCity(trimToNull(request.city()));
        job.setAddress(trimToNull(request.address()));
        job.setCountry(request.country().trim());
        job.setRemote(request.remote());
        applySalary(job, request);
        job.setHfMinYears(request.hfMinYears());
        job.setHfCountry(trimToNull(request.hfCountry()));
        job.setCutoffScore(request.cutoffScore());
        job.setSkills(buildSkills(job, request));
    }

    private void applySalary(Job job, JobRequest request) {
        SalaryType type = parseSalaryType(request.salaryType());
        BigDecimal min = request.salaryMin();
        BigDecimal max = request.salaryMax();
        String currency = request.salaryCurrency() == null ? "" : request.salaryCurrency().trim().toUpperCase();
        if (currency.isEmpty()) {
            currency = "USD";
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw ApiException.badRequest("INVALID_SALARY", "Salary currency must be a 3-letter code.");
        }
        SalaryPeriod period = parseSalaryPeriod(request.salaryPeriod());
        boolean negotiable = request.salaryNegotiable() == null || request.salaryNegotiable();

        if (type == SalaryType.negotiable) {
            min = null;
            max = null;
            negotiable = true;
        } else if (type == SalaryType.fixed) {
            if (min == null || min.signum() < 0) {
                throw ApiException.badRequest("INVALID_SALARY", "A fixed salary requires a non-negative amount.");
            }
            max = null;
        } else {
            if (min == null || max == null) {
                throw ApiException.badRequest("INVALID_SALARY", "A salary range requires both a minimum and a maximum.");
            }
            if (min.signum() < 0 || max.compareTo(min) < 0) {
                throw ApiException.badRequest("INVALID_SALARY", "The salary maximum must be greater than or equal to the minimum.");
            }
        }

        job.setSalaryType(type);
        job.setSalaryMin(min);
        job.setSalaryMax(max);
        job.setSalaryCurrency(currency);
        job.setSalaryPeriod(period);
        job.setSalaryNegotiable(negotiable);
    }

    private SalaryType parseSalaryType(String value) {
        if (value == null || value.isBlank()) {
            return SalaryType.negotiable;
        }
        try {
            return SalaryType.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_SALARY", "Salary type must be fixed, range or negotiable.");
        }
    }

    private SalaryPeriod parseSalaryPeriod(String value) {
        if (value == null || value.isBlank()) {
            return SalaryPeriod.year;
        }
        try {
            return SalaryPeriod.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_SALARY", "Salary period must be year, month or hour.");
        }
    }

    private List<JobSkill> buildSkills(Job job, JobRequest request) {
        List<UUID> required = request.requiredSkillIds() == null ? List.of() : request.requiredSkillIds();
        List<UUID> niceToHave = request.niceToHaveSkillIds() == null ? List.of() : request.niceToHaveSkillIds();
        List<UUID> mustHave = request.mustHaveSkillIds() == null ? List.of() : request.mustHaveSkillIds();

        if (!new HashSet<>(required).containsAll(mustHave)) {
            throw ApiException.badRequest("MUST_HAVE_NOT_REQUIRED", "Must-have skills must be required skills.");
        }

        Set<UUID> allIds = new HashSet<>();
        allIds.addAll(required);
        allIds.addAll(niceToHave);

        Map<UUID, Skill> skillMap = Map.of();
        if (!allIds.isEmpty()) {
            skillMap = skillRepository.findAllById(allIds).stream()
                    .collect(Collectors.toMap(Skill::getId, Function.identity()));
            if (skillMap.size() != allIds.size()) {
                throw ApiException.badRequest("SKILL_NOT_FOUND", "One or more skills do not exist.");
            }
        }

        List<JobSkill> skills = new ArrayList<>();
        for (UUID id : required) {
            JobSkill js = new JobSkill();
            js.setJob(job);
            js.setSkill(skillMap.get(id));
            js.setKind(JobSkillKind.required);
            js.setMustHave(mustHave.contains(id));
            skills.add(js);
        }
        for (UUID id : niceToHave) {
            JobSkill js = new JobSkill();
            js.setJob(job);
            js.setSkill(skillMap.get(id));
            js.setKind(JobSkillKind.nice_to_have);
            js.setMustHave(false);
            skills.add(js);
        }
        return skills;
    }

    private EmployerJobDetail toEmployerDetail(Job job) {
        List<EmployerJobDetail.SkillRef> required = new ArrayList<>();
        List<EmployerJobDetail.SkillRef> niceToHave = new ArrayList<>();
        for (JobSkill js : job.getSkills()) {
            if (js.getKind() == JobSkillKind.required) {
                required.add(new EmployerJobDetail.SkillRef(
                        js.getSkill().getId(), js.getSkill().getName(), js.isMustHave()));
            } else {
                niceToHave.add(new EmployerJobDetail.SkillRef(
                        js.getSkill().getId(), js.getSkill().getName(), false));
            }
        }
        List<JobRequiredAssessment> requirements = requiredAssessmentRepository.findByJobId(job.getId());
        Map<UUID, Assessment> assessments = requirements.isEmpty() ? Map.of()
                : assessmentRepository.findAllById(requirements.stream()
                        .map(JobRequiredAssessment::getAssessmentId).toList()).stream()
                .collect(Collectors.toMap(Assessment::getId, Function.identity()));
        List<EmployerJobDetail.RequiredAssessmentRef> requiredAssessments = requirements.stream()
                .sorted(java.util.Comparator.comparing(
                        r -> assessments.containsKey(r.getAssessmentId())
                                ? assessments.get(r.getAssessmentId()).getTitle() : ""))
                .map(r -> new EmployerJobDetail.RequiredAssessmentRef(
                        r.getAssessmentId(),
                        assessments.containsKey(r.getAssessmentId())
                                ? assessments.get(r.getAssessmentId()).getTitle() : null,
                        r.getMinScore()))
                .toList();
        return new EmployerJobDetail(
                job.getId(), job.getTitle(), job.getDescription(), job.getRequirements(),
                job.getExperienceLevel(), job.getCity(), job.getAddress(), job.getCountry(), job.isRemote(),
                required, niceToHave,
                job.getHfMinYears(), job.getHfCountry(), job.getCutoffScore(),
                job.getStatus(), job.getPublishedAt(),
                job.getSalaryType(), job.getSalaryMin(), job.getSalaryMax(),
                job.getSalaryCurrency(), job.getSalaryPeriod(), job.isSalaryNegotiable(),
                questionRepository.findByJobIdOrderByPositionAsc(job.getId()).stream()
                        .map(JobScreeningQuestion::getText)
                        .toList(),
                requiredAssessments);
    }

    private PublicJobListItem toPublicListItem(Job job) {
        List<String> topSkills = job.getSkills().stream()
                .sorted(java.util.Comparator.comparingInt(js -> js.getKind() == JobSkillKind.required ? 0 : 1))
                .limit(5)
                .map(js -> js.getSkill().getName())
                .toList();
        Company company = resolvePublicCompany(job.getCompanyId());
        return new PublicJobListItem(
                job.getId(), job.getTitle(), job.getCity(), job.getCountry(), job.isRemote(),
                job.getExperienceLevel(), topSkills, job.getPublishedAt(),
                company == null ? null : company.getName(),
                company == null ? null : company.getId(),
                company == null ? null : company.getLogoUrl(),
                job.getSalaryType(), job.getSalaryMin(), job.getSalaryMax(),
                job.getSalaryCurrency(), job.getSalaryPeriod(), job.isSalaryNegotiable());
    }

    private PublicJobDetail toPublicDetail(Job job) {
        List<String> required = new ArrayList<>();
        List<String> niceToHave = new ArrayList<>();
        for (JobSkill js : job.getSkills()) {
            if (js.getKind() == JobSkillKind.required) {
                required.add(js.getSkill().getName());
            } else {
                niceToHave.add(js.getSkill().getName());
            }
        }
        Company company = resolvePublicCompany(job.getCompanyId());
        return new PublicJobDetail(
                job.getId(), job.getTitle(), job.getDescription(), job.getRequirements(),
                job.getCity(), job.getAddress(), job.getCountry(), job.isRemote(), job.getExperienceLevel(),
                required, niceToHave, job.getPublishedAt(),
                company == null ? null : company.getName(),
                company == null ? null : company.getId(),
                company == null ? null : company.getLogoUrl(),
                job.getSalaryType(), job.getSalaryMin(), job.getSalaryMax(),
                job.getSalaryCurrency(), job.getSalaryPeriod(), job.isSalaryNegotiable());
    }

    private Company resolvePublicCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .filter(Company::isShowNamePublicly)
                .orElse(null);
    }

    private ExperienceLevel parseLevel(String value) {
        try {
            return ExperienceLevel.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_LEVEL", "experienceLevel must be one of entry, mid, senior, lead.");
        }
    }

    private JobStatus parseStatus(String value) {
        try {
            return JobStatus.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_STATUS", "status must be one of draft, active, paused, closed.");
        }
    }

    private boolean isComplete(Company company) {
        return notBlank(company.getName()) && notBlank(company.getIndustry()) && notBlank(company.getSize());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

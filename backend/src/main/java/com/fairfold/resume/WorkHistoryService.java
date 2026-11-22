package com.fairfold.resume;

import com.fairfold.common.errors.ApiException;
import com.fairfold.resume.dto.WorkHistoryRequest;
import com.fairfold.resume.dto.WorkHistoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WorkHistoryService {

    private final WorkHistoryRepository repository;

    public WorkHistoryService(WorkHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WorkHistoryResponse> list(UUID userId) {
        return repository.findByUserIdOrderByStartMonthDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WorkHistoryResponse create(UUID userId, WorkHistoryRequest request) {
        WorkHistory row = new WorkHistory();
        row.setUserId(userId);
        apply(row, request);
        return toResponse(repository.save(row));
    }

    @Transactional
    public WorkHistoryResponse update(UUID userId, UUID id, WorkHistoryRequest request) {
        WorkHistory row = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("WORK_HISTORY_NOT_FOUND", "Work history entry not found."));
        apply(row, request);
        return toResponse(repository.save(row));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        WorkHistory row = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("WORK_HISTORY_NOT_FOUND", "Work history entry not found."));
        repository.delete(row);
    }

    public void apply(WorkHistory row, WorkHistoryRequest request) {
        String title = clean(request.title());
        if (title == null) {
            throw ApiException.badRequest("TITLE_REQUIRED", "Give this role a title.");
        }
        row.setTitle(limit(title, 255));
        row.setEmployer(limit(clean(request.employer()), 255));
        LocalDate start = MonthFormat.parse(request.startMonth(), "Start month");
        LocalDate end = MonthFormat.parse(request.endMonth(), "End month");
        row.setStartMonth(start);
        row.setEndMonth(request.isCurrent() ? null : end);
        row.setCurrent(request.isCurrent());
        List<String> skills = new ArrayList<>();
        if (request.skillsUsed() != null) {
            for (String skill : request.skillsUsed()) {
                String value = clean(skill);
                if (value != null && skills.size() < 30) {
                    skills.add(limit(value, 100));
                }
            }
        }
        row.setSkillsUsed(skills);
    }

    public WorkHistoryResponse toResponse(WorkHistory row) {
        return new WorkHistoryResponse(
                row.getId(), row.getTitle(), row.getEmployer(),
                MonthFormat.format(row.getStartMonth()), MonthFormat.format(row.getEndMonth()),
                row.isCurrent(), row.getSkillsUsed());
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}

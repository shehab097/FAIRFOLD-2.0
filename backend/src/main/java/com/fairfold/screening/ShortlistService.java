package com.fairfold.screening;

import com.fairfold.application.Application;
import com.fairfold.application.ApplicationRepository;
import com.fairfold.common.paging.PageResponse;
import com.fairfold.job.Job;
import com.fairfold.job.JobRepository;
import com.fairfold.screening.dto.ShortlistedCandidate;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShortlistService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public ShortlistService(ApplicationRepository applicationRepository,
                            JobRepository jobRepository,
                            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ShortlistedCandidate> list(UUID companyId, Pageable pageable) {
        Page<Application> page = applicationRepository.findShortlistedForCompany(companyId, pageable);
        Map<UUID, Job> jobs = page.getContent().isEmpty() ? Map.of()
                : jobRepository.findAllById(page.getContent().stream()
                        .map(Application::getJobId).toList()).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<UUID, User> candidates = page.getContent().isEmpty() ? Map.of()
                : userRepository.findAllById(page.getContent().stream()
                        .map(Application::getCandidateId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<ShortlistedCandidate> items = page.getContent().stream()
                .map(application -> {
                    Job job = jobs.get(application.getJobId());
                    User candidate = candidates.get(application.getCandidateId());
                    return new ShortlistedCandidate(
                            application.getId(),
                            application.getJobId(),
                            job == null ? null : job.getTitle(),
                            candidate == null ? null
                                    : (candidate.getFirstName() + " " + candidate.getLastName()).trim(),
                            candidate == null ? null : candidate.getEmail(),
                            application.getScore(),
                            application.getStatus().name(),
                            application.getShortlistedAt());
                })
                .toList();
        return PageResponse.of(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}

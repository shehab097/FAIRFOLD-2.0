package com.fairfold.job;

import com.fairfold.common.paging.PageResponse;
import com.fairfold.job.dto.PublicJobDetail;
import com.fairfold.job.dto.PublicJobListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Public job board")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "List active jobs with search and filters")
    public PageResponse<PublicJobListItem> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) String level,
            @PageableDefault(size = 20) Pageable pageable) {
        return jobService.searchPublic(q, location, country, salaryMin, salaryMax, remote, level, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active job")
    public PublicJobDetail get(@PathVariable UUID id) {
        return jobService.getPublicJob(id);
    }
}

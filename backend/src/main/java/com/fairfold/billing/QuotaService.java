package com.fairfold.billing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

/** FR-048 quota: AI screening items completed in the current UTC month. */
@Service
public class QuotaService {

    private final JdbcTemplate jdbc;
    private final PlanService planService;
    private final Clock clock;

    public QuotaService(JdbcTemplate jdbc, PlanService planService, Clock clock) {
        this.jdbc = jdbc;
        this.planService = planService;
        this.clock = clock;
    }

    public record QuotaInfo(long used, int quota, String planName, Instant endsAt) {}

    public QuotaInfo forCompany(UUID companyId) {
        Plan plan = planService.effectivePlan(companyId);
        CompanySubscription subscription = planService.activeSubscription(companyId);
        return new QuotaInfo(usedThisMonth(companyId), plan.getMonthlyScreenings(),
                plan.getName(), subscription == null ? null : subscription.getEndsAt());
    }

    public long usedThisMonth(UUID companyId) {
        YearMonth month = YearMonth.now(clock.withZone(ZoneOffset.UTC));
        LocalDate start = month.atDay(1);
        Timestamp from = Timestamp.from(start.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp to = Timestamp.from(start.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM screening_items si "
                        + "JOIN screening_runs sr ON sr.id = si.run_id "
                        + "JOIN jobs j ON j.id = sr.job_id "
                        + "WHERE j.company_id = ? AND sr.mode = 'ai' AND si.status = 'done' "
                        + "AND si.updated_at >= ? AND si.updated_at < ?",
                Long.class, companyId.toString(), from, to);
        return count == null ? 0 : count;
    }

    public boolean wouldExceed(UUID companyId, int additional) {
        QuotaInfo info = forCompany(companyId);
        return info.used() + additional > info.quota();
    }
}

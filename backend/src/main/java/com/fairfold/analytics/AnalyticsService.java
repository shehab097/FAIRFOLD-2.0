package com.fairfold.analytics;

import com.fairfold.analytics.dto.AnalyticsResponse;
import com.fairfold.common.errors.ApiException;
import com.fairfold.job.JobRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** FR-035: company analytics from applications and status history. */
@Service
public class AnalyticsService {

    private final JdbcTemplate jdbc;
    private final JobRepository jobRepository;
    private final Clock clock;

    public AnalyticsService(JdbcTemplate jdbc, JobRepository jobRepository, Clock clock) {
        this.jdbc = jdbc;
        this.jobRepository = jobRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(UUID companyId, UUID jobId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(clock.withZone(ZoneOffset.UTC))
                : to;
        LocalDate start = from == null ? end.minusDays(89) : from;
        if (start.isAfter(end)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "from must be before to.");
        }
        if (jobId != null) {
            jobRepository.findByIdAndCompanyId(jobId, companyId)
                    .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Job not found."));
        }
        Timestamp fromTs = Timestamp.from(start.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp toTs = Timestamp.from(end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        List<Object> args = new ArrayList<>(List.of(companyId.toString(), fromTs, toTs));
        String jobClause = "";
        if (jobId != null) {
            jobClause = " AND a.job_id = ?";
            args.add(jobId.toString());
        }

        String base = " FROM applications a JOIN jobs j ON j.id = a.job_id "
                + "WHERE j.company_id = ? AND a.created_at >= ? AND a.created_at < ?" + jobClause;

        long applied = scalar("SELECT COUNT(*) " + base, args);
        long screened = scalar("SELECT SUM(CASE WHEN EXISTS (SELECT 1 FROM application_status_history h "
                + "WHERE h.application_id = a.id AND h.to_status = 'screened') THEN 1 ELSE 0 END) "
                + base, args);
        long shortlisted = scalar("SELECT SUM(CASE WHEN EXISTS (SELECT 1 FROM application_status_history h "
                + "WHERE h.application_id = a.id AND h.to_status = 'shortlisted') THEN 1 ELSE 0 END) "
                + base, args);
        long interviewed = scalar("SELECT SUM(CASE WHEN EXISTS (SELECT 1 FROM application_status_history h "
                + "WHERE h.application_id = a.id AND h.to_status = 'interview') THEN 1 ELSE 0 END) "
                + base, args);
        long hired = scalar("SELECT SUM(CASE WHEN EXISTS (SELECT 1 FROM application_status_history h "
                + "WHERE h.application_id = a.id AND h.to_status = 'hired') THEN 1 ELSE 0 END) "
                + base, args);
        List<AnalyticsResponse.FunnelStage> stages = List.of(
                new AnalyticsResponse.FunnelStage("applied", applied),
                new AnalyticsResponse.FunnelStage("screened", screened),
                new AnalyticsResponse.FunnelStage("shortlisted", shortlisted),
                new AnalyticsResponse.FunnelStage("interview", interviewed),
                new AnalyticsResponse.FunnelStage("hired", hired));

        List<AnalyticsResponse.TimeToHire> timeToHire = jdbc.query(
                "SELECT DATE_FORMAT(h.created_at, '%Y-%m') AS month, COUNT(*) AS hires, "
                        + "AVG(TIMESTAMPDIFF(SECOND, a.created_at, h.created_at)) / 86400.0 AS avg_days "
                        + "FROM application_status_history h JOIN applications a ON a.id = h.application_id "
                        + "JOIN jobs j ON j.id = a.job_id "
                        + "WHERE j.company_id = ? AND a.created_at >= ? AND a.created_at < ?"
                        + jobClause + " AND h.to_status = 'hired' "
                        + "GROUP BY month ORDER BY month",
                (rs, rowNum) -> new AnalyticsResponse.TimeToHire(
                        rs.getString("month"), rs.getLong("hires"),
                        Math.round(rs.getDouble("avg_days") * 10.0) / 10.0),
                args.toArray());

        List<AnalyticsResponse.Override> overrides = jdbc.query(
                "SELECT a.updated_at AS changed_at, "
                        + "COALESCE(CONCAT(u.first_name, ' ', u.last_name), 'Former user') AS user_name, "
                        + "j.title AS job_title, ca.alias AS alias, a.override_direction, a.decision_reason "
                        + "FROM applications a JOIN jobs j ON j.id = a.job_id "
                        + "LEFT JOIN users u ON u.id = a.decided_by "
                        + "LEFT JOIN candidate_aliases ca ON ca.candidate_id = a.candidate_id "
                        + "AND ca.company_id = j.company_id "
                        + "WHERE j.company_id = ? AND a.created_at >= ? AND a.created_at < ?"
                        + jobClause + " AND a.override_direction <> 'none' "
                        + "ORDER BY a.updated_at DESC",
                (rs, rowNum) -> new AnalyticsResponse.Override(
                        rs.getTimestamp("changed_at").toInstant(),
                        rs.getString("user_name"), rs.getString("job_title"),
                        rs.getString("alias") == null ? "Applicant" : rs.getString("alias"),
                        rs.getString("override_direction"), rs.getString("decision_reason")),
                args.toArray());

        List<Object> pullArgs = new ArrayList<>(List.of(companyId.toString(), fromTs, toTs));
        String pullJob = "";
        if (jobId != null) {
            pullJob = " AND a.job_id = ?";
            pullArgs.add(jobId.toString());
        }
        Long pullBacks = jdbc.queryForObject(
                "SELECT COUNT(*) FROM applications a JOIN jobs j ON j.id = a.job_id "
                        + "WHERE j.company_id = ? AND a.pulled_back_at >= ? AND a.pulled_back_at < ?" + pullJob,
                Long.class, pullArgs.toArray());

        return new AnalyticsResponse(stages, timeToHire, overrides,
                pullBacks == null ? 0 : pullBacks);
    }

    private long scalar(String sql, List<Object> args) {
        Number value = jdbc.queryForObject(sql, Number.class, args.toArray());
        return value == null ? 0 : value.longValue();
    }
}

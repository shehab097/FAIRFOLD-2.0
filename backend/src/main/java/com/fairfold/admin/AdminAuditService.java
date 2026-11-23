package com.fairfold.admin;

import com.fairfold.admin.dto.AuditLogItem;
import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.common.paging.PageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Part 5.1: audit query and CSV export. */
@Service
public class AdminAuditService {

    private static final int MAX_CSV_ROWS = 50_000;
    private static final String CSV_HEADER =
            "id,createdAt,actorId,actorEmail,action,resourceType,resourceId,details";

    private final JdbcTemplate jdbc;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AdminAuditService(JdbcTemplate jdbc, AuditService auditService,
                             ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogItem> list(String action, String resourceType, String actorId,
                                           LocalDate from, LocalDate to, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        List<Object> args = new ArrayList<>();
        String where = where(action, resourceType, actorId, from, to, args);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log a " + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(safeSize);
        listArgs.add(safePage * safeSize);
        List<AuditLogItem> items = jdbc.query(
                "SELECT a.id, a.created_at, a.actor_id, u.email AS actor_email, a.action, "
                        + "a.resource_type, a.resource_id, a.details "
                        + "FROM audit_log a LEFT JOIN users u ON u.id = a.actor_id " + where
                        + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> item(rs), listArgs.toArray());
        return PageResponse.of(items, safePage, safeSize, total == null ? 0 : total);
    }

    @Transactional
    public void exportCsv(String action, String resourceType, String actorId,
                          LocalDate from, LocalDate to, Writer writer) {
        List<Object> args = new ArrayList<>();
        String where = where(action, resourceType, actorId, from, to, args);
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log a " + where, Long.class, args.toArray());
        long total = count == null ? 0 : count;
        auditService.record(null, "audit_exported", "audit_log", null,
                "{\"count\":" + total + "}");

        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(MAX_CSV_ROWS);
        try {
            writer.write(CSV_HEADER);
            writer.write("\n");
            jdbc.query(
                    "SELECT a.id, a.created_at, a.actor_id, u.email AS actor_email, a.action, "
                            + "a.resource_type, a.resource_id, a.details "
                            + "FROM audit_log a LEFT JOIN users u ON u.id = a.actor_id " + where
                            + " ORDER BY a.created_at DESC, a.id DESC LIMIT ?",
                    (ResultSet rs) -> {
                        try {
                            writer.write(csv(rs.getString("id")));
                            writer.write("," + csv(rs.getTimestamp("created_at") == null ? ""
                                    : rs.getTimestamp("created_at").toInstant().toString()));
                            writer.write("," + csv(rs.getString("actor_id")));
                            writer.write("," + csv(rs.getString("actor_email")));
                            writer.write("," + csv(rs.getString("action")));
                            writer.write("," + csv(rs.getString("resource_type")));
                            writer.write("," + csv(rs.getString("resource_id")));
                            writer.write("," + csv(rs.getString("details")));
                            writer.write("\n");
                        } catch (IOException e) {
                            throw new IllegalStateException("Could not write audit CSV", e);
                        }
                    },
                    listArgs.toArray());
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write audit CSV", e);
        }
    }

    private String where(String action, String resourceType, String actorId,
                         LocalDate from, LocalDate to, List<Object> args) {
        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (action != null && !action.isBlank()) {
            where.append(" AND a.action = ?");
            args.add(action.strip());
        }
        if (resourceType != null && !resourceType.isBlank()) {
            where.append(" AND a.resource_type = ?");
            args.add(resourceType.strip());
        }
        if (actorId != null && !actorId.isBlank()) {
            try {
                args.add(UUID.fromString(actorId.strip()).toString());
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("INVALID_ACTOR", "actorId must be a UUID.");
            }
            where.append(" AND a.actor_id = ?");
        }
        if (from != null) {
            where.append(" AND DATE(a.created_at) >= ?");
            args.add(java.sql.Date.valueOf(from));
        }
        if (to != null) {
            where.append(" AND DATE(a.created_at) <= ?");
            args.add(java.sql.Date.valueOf(to));
        }
        return where.toString();
    }

    private AuditLogItem item(ResultSet rs) throws SQLException {
        String rawDetails = rs.getString("details");
        JsonNode details;
        try {
            details = rawDetails == null ? objectMapper.createObjectNode()
                    : objectMapper.readTree(rawDetails);
        } catch (IOException e) {
            details = objectMapper.createObjectNode();
        }
        Timestamp created = rs.getTimestamp("created_at");
        return new AuditLogItem(
                UUID.fromString(rs.getString("id")),
                created == null ? null : created.toInstant(),
                rs.getString("actor_id") == null ? null : UUID.fromString(rs.getString("actor_id")),
                rs.getString("actor_email"),
                rs.getString("action"), rs.getString("resource_type"), rs.getString("resource_id"),
                details);
    }

    private String csv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

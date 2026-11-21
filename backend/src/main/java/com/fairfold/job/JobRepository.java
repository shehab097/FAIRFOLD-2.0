package com.fairfold.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndStatus(UUID id, JobStatus status);

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("select j from Job j where j.companyId = :companyId and (:status is null or j.status = :status) order by j.createdAt desc")
    Page<Job> findByCompany(@Param("companyId") UUID companyId, @Param("status") JobStatus status, Pageable pageable);

    /**
     * Public job board search. {@code q} is a non-null string (empty means "no
     * search"); {@code qEscaped} is {@code q} with %, _ and \ escaped for LIKE.
     */
    @Query(value = """
            SELECT j.* FROM jobs j
            WHERE j.status = 'active'
              AND (:level IS NULL OR j.experience_level = :level)
              AND (:remote IS NULL OR j.remote = :remote)
              AND (:location IS NULL
                   OR LOWER(j.city) LIKE CONCAT('%', LOWER(:location), '%')
                   OR LOWER(j.country) LIKE CONCAT('%', LOWER(:location), '%'))
              AND (
                  :q = ''
                  OR MATCH(j.title, j.description, j.requirements) AGAINST (:q IN NATURAL LANGUAGE MODE) > 0
                  OR EXISTS (SELECT 1 FROM job_skills js2 JOIN skills s2 ON s2.id = js2.skill_id
                             WHERE js2.job_id = j.id AND LOWER(s2.name) = LOWER(:q))
                  OR (CHAR_LENGTH(:q) < 3 AND j.title LIKE CONCAT('%', :qEscaped, '%'))
              )
            ORDER BY j.published_at DESC, j.id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM jobs j
            WHERE j.status = 'active'
              AND (:level IS NULL OR j.experience_level = :level)
              AND (:remote IS NULL OR j.remote = :remote)
              AND (:location IS NULL
                   OR LOWER(j.city) LIKE CONCAT('%', LOWER(:location), '%')
                   OR LOWER(j.country) LIKE CONCAT('%', LOWER(:location), '%'))
              AND (
                  :q = ''
                  OR MATCH(j.title, j.description, j.requirements) AGAINST (:q IN NATURAL LANGUAGE MODE) > 0
                  OR EXISTS (SELECT 1 FROM job_skills js2 JOIN skills s2 ON s2.id = js2.skill_id
                             WHERE js2.job_id = j.id AND LOWER(s2.name) = LOWER(:q))
                  OR (CHAR_LENGTH(:q) < 3 AND j.title LIKE CONCAT('%', :qEscaped, '%'))
              )
            """,
            nativeQuery = true)
    Page<Job> searchActive(
            @Param("q") String q,
            @Param("qEscaped") String qEscaped,
            @Param("location") String location,
            @Param("remote") Boolean remote,
            @Param("level") String level,
            Pageable pageable);
}

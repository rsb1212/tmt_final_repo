package com.testmgmt.repository;

import com.testmgmt.entity.PdCrCall;
import com.testmgmt.enums.PdCrCallStatus;
import com.testmgmt.enums.PdCrCallType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdCrCallRepository extends JpaRepository<PdCrCall, UUID> {

    Optional<PdCrCall> findByChildCallIdAndProjectId(String childCallId, UUID projectId);

    List<PdCrCall> findByActiveTrue();

    Page<PdCrCall> findByActiveTrue(Pageable pageable);

    List<PdCrCall> findByProjectIdAndActiveTrue(UUID projectId);

    /**
     * Flexible, null-friendly filter used by the list endpoint. Any of the
     * filter params may be null to mean "don't filter on this field".
     *
     * NOTE: {@code :owner} and {@code :q} are explicitly CAST to string. On
     * PostgreSQL, a null String parameter that is only ever used as an argument
     * to LOWER()/LIKE cannot have its type inferred by Hibernate, so it is bound
     * as an untyped null which Postgres resolves to {@code bytea}. That makes
     * {@code lower(?)} fail with "function lower(bytea) does not exist"
     * (SQLState 42883) and the whole endpoint returns HTTP 500. The CAST forces
     * a known VARCHAR type so the query works whether the value is null or not.
     */
    @Query("""
            SELECT c FROM PdCrCall c
            WHERE c.active = true
              AND (:projectId IS NULL OR c.project.id = :projectId)
              AND (:callType  IS NULL OR c.callType   = :callType)
              AND (:status    IS NULL OR c.status     = :status)
              AND (CAST(:owner AS string) IS NULL OR LOWER(c.applicationOwner) = LOWER(CAST(:owner AS string)))
              AND (CAST(:q AS string) IS NULL OR
                   LOWER(c.childCallId)      LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR
                   LOWER(c.issueDescription) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR
                   LOWER(c.uatSpoc)          LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<PdCrCall> search(@Param("projectId") UUID projectId,
                          @Param("callType") PdCrCallType callType,
                          @Param("status") PdCrCallStatus status,
                          @Param("owner") String owner,
                          @Param("q") String q,
                          Pageable pageable);

    long countByActiveTrue();

    long countByActiveTrueAndCallType(PdCrCallType callType);

    long countByActiveTrueAndStatus(PdCrCallStatus status);
}

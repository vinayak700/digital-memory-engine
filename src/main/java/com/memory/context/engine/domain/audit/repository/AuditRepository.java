package com.memory.context.engine.domain.audit.repository;

import com.memory.context.engine.domain.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repository for audit events.
 * Read operations only - events are append-only.
 */
public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

    /**
     * Find all audit events for a specific memory.
     */
    List<AuditEvent> findByMemoryIdOrderByOccurredAtDesc(Long memoryId);

    /**
     * Find all audit events for a user with pagination.
     */
    Page<AuditEvent> findByUserIdOrderByOccurredAtDesc(String userId, Pageable pageable);

    /**
     * Find events by type within a time range.
     */
    List<AuditEvent> findByEventTypeAndOccurredAtBetween(
            String eventType,
            Instant start,
            Instant end);

    @Modifying
    @Query("DELETE FROM AuditEvent a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}

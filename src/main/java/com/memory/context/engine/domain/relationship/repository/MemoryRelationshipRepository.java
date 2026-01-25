package com.memory.context.engine.domain.relationship.repository;

import com.memory.context.engine.domain.relationship.entity.MemoryRelationship;
import com.memory.context.engine.domain.relationship.entity.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for memory relationships.
 */
public interface MemoryRelationshipRepository extends JpaRepository<MemoryRelationship, Long> {

    List<MemoryRelationship> findBySourceMemoryId(Long sourceMemoryId);

    List<MemoryRelationship> findByTargetMemoryId(Long targetMemoryId);

    List<MemoryRelationship> findBySourceMemoryIdAndRelationshipType(
            Long sourceMemoryId,
            RelationshipType type);

    @Query("""
            SELECT r FROM MemoryRelationship r
            WHERE r.sourceMemory.id = :memoryId OR r.targetMemory.id = :memoryId
            """)
    List<MemoryRelationship> findAllConnected(Long memoryId);

    boolean existsBySourceMemoryIdAndTargetMemoryIdAndRelationshipType(
            Long sourceId,
            Long targetId,
            RelationshipType type);
}

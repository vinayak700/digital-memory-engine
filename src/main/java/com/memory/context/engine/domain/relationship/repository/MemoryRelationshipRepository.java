package com.memory.context.engine.domain.relationship.repository;

import com.memory.context.engine.domain.memory.entity.MemoryRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryRelationshipRepository extends JpaRepository<MemoryRelationship, Long> {
    List<MemoryRelationship> findByFromMemoryId(Long memoryId);
}

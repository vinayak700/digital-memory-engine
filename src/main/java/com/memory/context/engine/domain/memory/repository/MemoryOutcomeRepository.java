package com.memory.context.engine.domain.memory.repository;

import com.memory.context.engine.domain.memory.entity.MemoryOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryOutcomeRepository extends JpaRepository<MemoryOutcome, Long> {
    List<MemoryOutcome> findByMemoryId(Long memoryId);
}

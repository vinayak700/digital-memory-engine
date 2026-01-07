package com.memory.context.engine.domain.memory.repository;

import com.memory.context.engine.domain.memory.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findByUserIdAndArchivedFalse(String userId);
}

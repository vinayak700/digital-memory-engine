package com.memory.context.engine.domain.memory.repository;

import com.memory.context.engine.domain.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    Page<Memory> findByUserIdAndArchivedFalse(
            String userId,
            Pageable pageable);
}

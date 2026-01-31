package com.memory.context.engine.domain.memory.repository;

import com.memory.context.engine.domain.memory.entity.Memory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    Page<Memory> findByUserIdAndArchivedFalse(
            String userId,
            Pageable pageable);

    List<Memory> findAllByUserId(String userId);

    @Modifying
    @Query("DELETE FROM Memory m WHERE m.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}

package com.memory.context.engine.domain.memory.service;

import com.memory.context.engine.domain.common.exception.ResourceNotFoundException;
import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.entity.MemoryOutcome;
import com.memory.context.engine.domain.memory.repository.MemoryOutcomeRepository;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryOutcomeService {

    private final MemoryOutcomeRepository outcomeRepository;
    private final MemoryRepository memoryRepository;

    @Transactional
    public void recordFeedback(Long memoryId, int satisfactionScore, String summary) {
        log.info("Recording feedback for memory {}: score={}", memoryId, satisfactionScore);

        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found"));

        if (satisfactionScore < 1 || satisfactionScore > 5) {
            throw new IllegalArgumentException("Satisfaction score must be between 1 and 5");
        }

        // Use builder if available, else constructor/setters
        // Since MemoryOutcome has @NoArgsConstructor(access = PROTECTED), we rely on a
        // builder or we need to add one.
        // Let's check MemoryOutcome entity structure from previous view.
        // It didn't show @Builder. I will assume I need to add @Builder or use a
        // constructor.
        // Assuming I can update the entity or use reflection/mapper.
        // Best practice: Update Entity to have @Builder.

        // For now, I will use a custom static method or setter if accessible.
        // Wait, the entity had protected no-args.
        // I should update MemoryOutcome to have @Builder and @AllArgsConstructor.

        // I will implement the service assuming the entity is updated.
        // I will update the entity in the next step.
    }
}

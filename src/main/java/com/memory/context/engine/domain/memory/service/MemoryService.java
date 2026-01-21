package com.memory.context.engine.domain.memory.service;

import com.memory.context.engine.domain.memory.api.dto.CreateMemoryRequest;
import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Transactional
    public MemoryResponse createMemory(CreateMemoryRequest request) {
        Memory memory = new Memory();
        memory.setTitle(request.getTitle());
        memory.setContent(request.getContent());
        memory.setUserId(request.getUserId());
        memory.setImportanceScore(request.getImportanceScore());
        memory.setContext(request.getContext());

        Memory saved = memoryRepository.save(memory);

        OffsetDateTime createdAt = memory.getCreatedAt() != null
                        ? memory.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null;

        return new MemoryResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getContent(),
                saved.getContext(),
                saved.getImportanceScore(),
                saved.isArchived(),
                createdAt
        );
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> getActiveMemories(String userId) {
        return memoryRepository.findByUserIdAndArchivedFalse(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MemoryResponse toResponse(Memory memory) {
        return new MemoryResponse(
                memory.getId(),
                memory.getTitle(),
                memory.getContent(),
                memory.getContext(),
                memory.getImportanceScore(),
                memory.isArchived(),
                memory.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    @Transactional
    public void archiveMemory(Long id) {
        Memory memory = memoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Memory not found"));
        if (!memory.isArchived()) {
            memory.setArchived(true);
        }
    }

}

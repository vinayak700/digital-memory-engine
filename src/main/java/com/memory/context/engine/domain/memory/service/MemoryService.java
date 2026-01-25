package com.memory.context.engine.domain.memory.service;

import com.memory.context.engine.domain.common.exception.AccessDeniedException;
import com.memory.context.engine.domain.common.exception.InvalidMemoryStateException;
import com.memory.context.engine.domain.common.exception.ResourceNotFoundException;
import com.memory.context.engine.domain.memory.api.dto.CreateMemoryRequest;
import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.api.dto.UpdateMemoryRequest;
import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

import static com.memory.context.engine.domain.common.util.MemoryUtils.setIfNotNull;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    // ==================================================
    // CREATE
    // ==================================================

    @Transactional
    public MemoryResponse createMemory(CreateMemoryRequest request) {
        Memory memory = new Memory();
        memory.setTitle(request.getTitle());
        memory.setContent(request.getContent());
        memory.setUserId(currentUser());
        memory.setImportanceScore(request.getImportanceScore());
        memory.setContext(request.getContext());
        memory.setArchived(false);

        Memory saved = memoryRepository.save(memory);
        return toResponse(saved);
    }

    // ==================================================
    // READ
    // ==================================================

    @Transactional(readOnly = true)
    public MemoryResponse getMemory(Long id) {
        Memory memory = loadAndAuthorize(id);
        return toResponse(memory);
    }

    @Transactional(readOnly = true)
    public Page<MemoryResponse> getActiveMemories(Pageable pageable) {
        return memoryRepository
                .findByUserIdAndArchivedFalse(currentUser(), pageable)
                .map(this::toResponse);
    }

    // ==================================================
    // UPDATE
    // ==================================================

    @Transactional
    public MemoryResponse updateMemory(Long id, UpdateMemoryRequest request) {
        Memory memory = loadAndAuthorize(id);

        if (memory.isArchived()) {
            throw new InvalidMemoryStateException(
                    "Archived memory cannot be modified");
        }

        setIfNotNull(request.getTitle(), memory::setTitle);
        setIfNotNull(request.getContent(), memory::setContent);
        setIfNotNull(request.getImportanceScore(), memory::setImportanceScore);
        setIfNotNull(request.getContext(), memory::setContext);

        return toResponse(memory);
    }

    // ==================================================
    // ARCHIVE
    // ==================================================

    @Transactional
    public void archiveMemory(Long id) {
        Memory memory = loadAndAuthorize(id);

        if (memory.isArchived()) {
            return; // idempotent
        }

        memory.setArchived(true);
    }

    // ==================================================
    // INTERNALS
    // ==================================================

    private Memory loadAndAuthorize(Long memoryId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found"));

        if (!memory.getUserId().equals(currentUser())) {
            throw new AccessDeniedException(
                    "You do not have access to this memory");
        }

        return memory;
    }

    private String currentUser() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    private MemoryResponse toResponse(Memory memory) {
        return new MemoryResponse(
                memory.getId(),
                memory.getTitle(),
                memory.getContent(),
                memory.getContext(),
                memory.getImportanceScore(),
                memory.isArchived(),
                memory.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
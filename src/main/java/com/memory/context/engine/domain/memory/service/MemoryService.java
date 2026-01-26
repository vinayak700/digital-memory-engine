package com.memory.context.engine.domain.memory.service;

import com.memory.context.engine.domain.common.exception.AccessDeniedException;
import com.memory.context.engine.domain.common.exception.InvalidMemoryStateException;
import com.memory.context.engine.domain.common.exception.ResourceNotFoundException;
import com.memory.context.engine.domain.intelligence.KeywordExtractionService;
import com.memory.context.engine.domain.memory.api.dto.CreateMemoryRequest;
import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.api.dto.UpdateMemoryRequest;
import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.event.MemoryArchivedEvent;
import com.memory.context.engine.domain.memory.event.MemoryCreatedEvent;
import com.memory.context.engine.domain.memory.event.MemoryUpdatedEvent;
import com.memory.context.engine.domain.memory.mapper.MemoryMapper;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.infrastructure.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service layer for Memory operations.
 * Handles business logic, authorization, caching, events, and transaction
 * management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryMapper memoryMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final KeywordExtractionService keywordExtractionService;

    // ==================================================
    // CREATE
    // ==================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.MEMORY_LISTS, allEntries = true),
            @CacheEvict(value = "search-results", allEntries = true)
    })
    public MemoryResponse createMemory(CreateMemoryRequest request) {
        String userId = currentUser();
        log.info("Creating memory for user: {}, title: {}", userId, request.getTitle());

        // Auto-extract context metadata if not provided
        Map<String, Object> context = request.getContext();
        if (context == null || context.isEmpty()) {
            log.debug("Auto-extracting context metadata for memory");
            context = keywordExtractionService.extractContextMetadata(
                    request.getTitle(),
                    request.getContent());
        }

        Memory memory = Memory.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .importanceScore(request.getImportanceScore())
                .context(context)
                .archived(false)
                .build();

        Memory saved = memoryRepository.save(memory);
        log.debug("Memory created with id: {}, keywords: {}", saved.getId(),
                context.get("keywords"));

        // Publish domain event
        eventPublisher.publishEvent(new MemoryCreatedEvent(
                saved.getId(),
                userId,
                saved.getTitle(),
                saved.getImportanceScore()));

        return memoryMapper.toResponse(saved);
    }

    // ==================================================
    // READ
    // ==================================================

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MEMORIES, key = "#id")
    public MemoryResponse getMemory(Long id) {
        log.debug("Fetching memory id: {} for user: {}", id, currentUser());
        Memory memory = loadAndAuthorize(id);
        return memoryMapper.toResponse(memory);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MEMORY_LISTS, key = "'user:' + #root.target.currentUser() + ':page:' + #pageable.pageNumber")
    public List<MemoryResponse> getActiveMemories(Pageable pageable) {
        String userId = currentUser();
        log.debug("Fetching active memories for user: {}, page: {}", userId, pageable.getPageNumber());

        return memoryRepository
                .findByUserIdAndArchivedFalse(userId, pageable)
                .map(memoryMapper::toResponse).getContent();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.MEMORIES, key = "#id"),
            @CacheEvict(value = CacheNames.MEMORY_LISTS, allEntries = true),
            @CacheEvict(value = "search-results", allEntries = true)
    })
    public MemoryResponse updateMemory(Long id, UpdateMemoryRequest request) {
        String userId = currentUser();
        log.info("Updating memory id: {} for user: {}", id, userId);
        Memory memory = loadAndAuthorize(id);

        if (memory.isArchived()) {
            log.warn("Attempted to update archived memory id: {}", id);
            throw new InvalidMemoryStateException("Archived memory cannot be modified");
        }

        // Track which fields are being updated
        Set<String> updatedFields = new HashSet<>();

        if (request.getTitle() != null) {
            memory.setTitle(request.getTitle());
            updatedFields.add("title");
        }
        if (request.getContent() != null) {
            memory.setContent(request.getContent());
            updatedFields.add("content");
        }
        if (request.getImportanceScore() != null) {
            memory.setImportanceScore(request.getImportanceScore());
            updatedFields.add("importanceScore");
        }
        if (request.getContext() != null) {
            memory.setContext(request.getContext());
            updatedFields.add("context");
        }

        Memory saved = memoryRepository.save(memory);
        log.debug("Memory id: {} updated successfully", id);

        // Publish domain event
        eventPublisher.publishEvent(new MemoryUpdatedEvent(id, userId, updatedFields));

        return memoryMapper.toResponse(saved);
    }

    // ==================================================
    // ARCHIVE
    // ==================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.MEMORIES, key = "#id"),
            @CacheEvict(value = CacheNames.MEMORY_LISTS, allEntries = true),
            @CacheEvict(value = "search-results", allEntries = true)
    })
    public void archiveMemory(Long id) {
        String userId = currentUser();
        log.info("Archiving memory id: {} for user: {}", id, userId);
        Memory memory = loadAndAuthorize(id);

        if (memory.isArchived()) {
            log.debug("Memory id: {} already archived, skipping", id);
            return; // idempotent
        }

        memory.setArchived(true);
        memoryRepository.save(memory);
        log.debug("Memory id: {} archived successfully", id);

        // Publish domain event
        eventPublisher.publishEvent(new MemoryArchivedEvent(id, userId));
    }

    private Memory loadAndAuthorize(Long memoryId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> {
                    log.warn("Memory not found: {}", memoryId);
                    return new ResourceNotFoundException("Memory not found");
                });

        String userId = currentUser();
        if (!memory.getUserId().equals(userId)) {
            log.warn("Access denied: user {} attempted to access memory {} owned by {}",
                    userId, memoryId, memory.getUserId());
            throw new AccessDeniedException("You do not have access to this memory");
        }

        return memory;
    }

    public String currentUser() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }
}
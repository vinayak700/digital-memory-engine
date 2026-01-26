package com.memory.context.engine.domain.relationship.service;

import com.memory.context.engine.domain.common.exception.ResourceNotFoundException;
import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.domain.relationship.api.dto.CreateRelationshipRequest;
import com.memory.context.engine.domain.relationship.api.dto.RelatedMemoryDto;
import com.memory.context.engine.domain.relationship.entity.MemoryRelationship;
import com.memory.context.engine.domain.relationship.repository.MemoryRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for graph operations on memory relationships.
 * Provides relationship CRUD and traversal with caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private static final String CACHE_GRAPH = "graph-traversal";

    private final MemoryRelationshipRepository relationshipRepository;
    private final MemoryRepository memoryRepository;

    @Transactional
    @CacheEvict(value = CACHE_GRAPH, allEntries = true)
    public MemoryRelationship createRelationship(CreateRelationshipRequest request) {
        String userId = getCurrentUser();
        Memory source = memoryRepository.findById(request.getSourceMemoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Source memory not found"));
        Memory target = memoryRepository.findById(request.getTargetMemoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Target memory not found"));

        // Verify ownership
        if (!source.getUserId().equals(userId) || !target.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Memory not found");
        }

        MemoryRelationship relationship = MemoryRelationship.builder()
                .sourceMemory(source)
                .targetMemory(target)
                .relationshipType(request.getType())
                .strength(request.getStrength())
                .build();

        return relationshipRepository.save(relationship);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_GRAPH, key = "'related:' + #memoryId")
    public List<RelatedMemoryDto> getRelatedMemories(Long memoryId) {
        log.debug("Getting related memories for: {}", memoryId);

        List<MemoryRelationship> relationships = relationshipRepository.findAllConnected(memoryId);

        return relationships.stream()
                .map(r -> {
                    Memory related = r.getSourceMemory().getId().equals(memoryId)
                            ? r.getTargetMemory()
                            : r.getSourceMemory();
                    return RelatedMemoryDto.builder()
                            .memoryId(related.getId())
                            .title(related.getTitle())
                            .relationshipType(r.getRelationshipType())
                            .strength(r.getStrength())
                            .isOutgoing(r.getSourceMemory().getId().equals(memoryId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_GRAPH, key = "'traversal:' + #memoryId + ':' + #depth")
    public Set<Long> traverseGraph(Long memoryId, int depth) {
        log.debug("Traversing graph from memory: {} with depth: {}", memoryId, depth);

        Set<Long> visited = new HashSet<>();
        Set<Long> current = Set.of(memoryId);

        for (int i = 0; i < depth && !current.isEmpty(); i++) {
            Set<Long> next = new HashSet<>();
            for (Long id : current) {
                if (visited.add(id)) {
                    List<MemoryRelationship> rels = relationshipRepository.findAllConnected(id);
                    for (MemoryRelationship r : rels) {
                        next.add(r.getSourceMemory().getId());
                        next.add(r.getTargetMemory().getId());
                    }
                }
            }
            current = next;
        }

        visited.remove(memoryId); // Remove starting node
        return visited;
    }

    @Transactional
    @CacheEvict(value = CACHE_GRAPH, allEntries = true)
    public void deleteRelationship(Long relationshipId) {
        log.info("Deleting relationship: {}", relationshipId);
        relationshipRepository.deleteById(relationshipId);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

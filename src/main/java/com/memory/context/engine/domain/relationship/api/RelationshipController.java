package com.memory.context.engine.domain.relationship.api;

import com.memory.context.engine.domain.relationship.api.dto.CreateRelationshipRequest;
import com.memory.context.engine.domain.relationship.api.dto.RelatedMemoryDto;
import com.memory.context.engine.domain.relationship.entity.MemoryRelationship;
import com.memory.context.engine.domain.relationship.service.GraphService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST controller for relationship operations.
 */
@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
public class RelationshipController {

    private final GraphService graphService;

    @PostMapping
    public ResponseEntity<Void> createRelationship(@Valid @RequestBody CreateRelationshipRequest request) {
        graphService.createRelationship(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/memory/{memoryId}")
    public ResponseEntity<List<RelatedMemoryDto>> getRelatedMemories(@PathVariable Long memoryId) {
        return ResponseEntity.ok(graphService.getRelatedMemories(memoryId));
    }

    @GetMapping("/memory/{memoryId}/traverse")
    public ResponseEntity<Set<Long>> traverseGraph(
            @PathVariable Long memoryId,
            @RequestParam(defaultValue = "2") int depth) {
        return ResponseEntity.ok(graphService.traverseGraph(memoryId, Math.min(depth, 5)));
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> deleteRelationship(@PathVariable Long relationshipId) {
        graphService.deleteRelationship(relationshipId);
        return ResponseEntity.noContent().build();
    }
}

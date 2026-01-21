package com.memory.context.engine.domain.memory.api;

import com.memory.context.engine.domain.memory.api.dto.CreateMemoryRequest;
import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.service.MemoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Create a new memory
     */
    @PostMapping
    public ResponseEntity<MemoryResponse> createMemory(@Valid @RequestBody CreateMemoryRequest request) {
        MemoryResponse response = memoryService.createMemory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all active (non-archived) memories for a user
     */
    @GetMapping
    public ResponseEntity<List<MemoryResponse>> getMemories(@RequestParam("userId") String userId
    ) {
        List<MemoryResponse> memories = memoryService.getActiveMemories(userId);
        return ResponseEntity.ok(memories);
    }

    /**
     * Get a single memory by ID (ownership enforced later)
     */
//    @GetMapping("/{id}")
//    public ResponseEntity<MemoryResponse> getMemoryById(@PathVariable Long id) {
//        MemoryResponse memory = memoryService.getMemoryById(id);
//        return ResponseEntity.ok(memory);
//    }

//    /**
//     * Patch / update a memory
//     */
//    @PatchMapping("/{id}")
//    public ResponseEntity<MemoryResponse> updateMemory(@PathVariable Long id, @Valid @RequestBody UpdateMemoryRequest request
//    ) {
//        MemoryResponse updated = memoryService.updateMemory(id, request);
//        return ResponseEntity.ok(updated);
//    }

    /**
     * Archive a memory (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveMemory(@PathVariable Long id) {
        memoryService.archiveMemory(id);
        return ResponseEntity.noContent().build();
    }
}

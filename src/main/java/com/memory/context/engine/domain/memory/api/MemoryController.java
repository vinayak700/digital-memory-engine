package com.memory.context.engine.domain.memory.api;

import com.memory.context.engine.domain.memory.api.dto.CreateMemoryRequest;
import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.api.dto.UpdateMemoryRequest;
import com.memory.context.engine.domain.memory.service.MemoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @PostMapping
    public ResponseEntity<MemoryResponse> create(
            @Valid @RequestBody CreateMemoryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(memoryService.createMemory(request));
    }

    @GetMapping
    public ResponseEntity<List<MemoryResponse>> getMemories(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(memoryService.getActiveMemories(pageable));
    }

    @GetMapping("/{id}")
    public MemoryResponse get(@PathVariable Long id) {
        return memoryService.getMemory(id);
    }

    @PatchMapping("/{id}")
    public MemoryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemoryRequest request) {
        return memoryService.updateMemory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        memoryService.archiveMemory(id);
        return ResponseEntity.noContent().build();
    }
}

package com.memory.context.engine.domain.search.api;

import com.memory.context.engine.domain.search.api.dto.SearchRequest;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
import com.memory.context.engine.domain.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for semantic search operations.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<List<SearchResult>> search(@Valid @RequestBody SearchRequest request) {
        List<SearchResult> results = searchService.search(request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/similar/{memoryId}")
    public ResponseEntity<List<SearchResult>> findSimilar(
            @PathVariable Long memoryId,
            @RequestParam(defaultValue = "5") int limit) {
        List<SearchResult> results = searchService.findSimilar(memoryId, limit);
        return ResponseEntity.ok(results);
    }
}

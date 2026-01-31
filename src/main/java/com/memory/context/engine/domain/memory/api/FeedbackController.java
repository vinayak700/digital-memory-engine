package com.memory.context.engine.domain.memory.api;

import com.memory.context.engine.domain.memory.service.MemoryOutcomeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/memories")
@RequiredArgsConstructor
public class FeedbackController {

    private final MemoryOutcomeService outcomeService;

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable Long id,
            @RequestBody FeedbackRequest request) {

        outcomeService.recordFeedback(id, request.getScore(), request.getSummary());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class FeedbackRequest {
        private int score;
        private String summary;
    }
}

package com.memory.context.engine.domain.intelligence.api;

import com.memory.context.engine.domain.intelligence.AnswerResponse;
import com.memory.context.engine.domain.intelligence.AnswerSynthesisEngine;
import com.memory.context.engine.domain.intelligence.AskRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for the Intelligent Answer Engine.
 * Provides natural language Q&A interface over your memories.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ask")
@RequiredArgsConstructor
public class AskController {

    private final AnswerSynthesisEngine answerEngine;

    /**
     * Ask a question and get an intelligent answer synthesized from your memories.
     * 
     * Example:
     * POST /api/v1/ask
     * {"question": "What do I know about Java design patterns?"}
     * 
     * Response:
     * {
     * "question": "What do I know about Java design patterns?",
     * "answer": "Based on your memories...",
     * "confidence": 0.85,
     * "sources": [...]
     * }
     */
    @PostMapping
    public ResponseEntity<AnswerResponse> ask(@Valid @RequestBody AskRequest request) {
        log.info("Received question: {}", request.getQuestion());

        AnswerResponse response = answerEngine.ask(request.getQuestion());

        return ResponseEntity.ok(response);
    }

    /**
     * Quick ask via GET (for convenience).
     * Example: GET /api/v1/ask?q=What+are+my+priorities
     */
    @GetMapping
    public ResponseEntity<AnswerResponse> askGet(@RequestParam("q") String question) {
        log.info("Received question (GET): {}", question);

        AnswerResponse response = answerEngine.ask(question);

        return ResponseEntity.ok(response);
    }
}

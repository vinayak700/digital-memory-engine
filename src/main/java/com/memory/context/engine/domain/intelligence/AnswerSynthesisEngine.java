package com.memory.context.engine.domain.intelligence;

import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.domain.relationship.entity.MemoryRelationship;
import com.memory.context.engine.domain.relationship.repository.MemoryRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answer Synthesis Engine - The core intelligence of Digital Memory Engine.
 * Generates intelligent answers by analyzing and synthesizing relevant
 * memories.
 * No LLM required - uses classical NLP and graph algorithms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerSynthesisEngine {

    private final MemoryRepository memoryRepository;
    private final MemoryRelationshipRepository relationshipRepository;
    private final TextSimilarityService similarityService;
    private final KeywordExtractionService keywordService;
    private final JdbcTemplate jdbcTemplate;
    private final GeminiService geminiService;

    /**
     * Main entry point: Ask a question and get an intelligent answer.
     */
    public AnswerResponse ask(String question) {
        String userId = getCurrentUser();
        log.info("Processing question for user {}: '{}'", userId, question);

        // 1. Find relevant memories using full-text search
        List<ScoredMemory> relevantMemories = findRelevantMemories(question, userId, 10);

        // 2. Expand context via relationships
        List<ScoredMemory> expandedContext = expandContextViaRelationships(relevantMemories, userId);

        // 3. Synthesize answer from memories
        String synthesizedAnswer = synthesizeAnswer(question, relevantMemories, expandedContext);

        // 4. Build response with sources
        return AnswerResponse.builder()
                .question(question)
                .answer(synthesizedAnswer)
                .sources(relevantMemories.stream()
                        .map(m -> new SourceReference(m.memory.getId(), m.memory.getTitle(), m.score))
                        .collect(Collectors.toList()))
                .relatedMemories(expandedContext.stream()
                        .map(m -> m.memory.getId())
                        .collect(Collectors.toList()))
                .confidence(calculateConfidence(relevantMemories))
                .build();
    }

    /**
     * Find memories relevant to the question using PostgreSQL full-text search.
     */
    private List<ScoredMemory> findRelevantMemories(String question, String userId, int limit) {
        // Extract search terms
        // Extract search terms and ensure valid tsquery syntax
        List<String> keywords = keywordService.extractKeywords(question, 5);

        // Split any multi-word keywords and the original question to ensure we catch
        // all terms
        Set<String> searchTerms = new HashSet<>();
        searchTerms.addAll(keywords);
        // Also add individual words from the question to ensure broad recall
        String[] rawWords = question.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().split("\\s+");
        for (String word : rawWords) {
            if (word.length() > 2) { // Filter out very short words
                searchTerms.add(word);
            }
        }

        // Construct a valid to_tsquery string: "term1 | term2 | term3"
        String searchQuery = searchTerms.stream()
                .map(term -> term.trim().replaceAll("\\s+", " & ")) // Treat phrases as AND
                .filter(term -> !term.isBlank())
                .collect(Collectors.joining(" | "));

        if (searchQuery.isBlank()) {
            // Fallback for empty query
            searchQuery = "memory";
        }

        log.debug("Searching with formatted tsquery: {}", searchQuery);

        // Use PostgreSQL full-text search with ranking
        // We use to_tsquery to support the OR (|) operator constructed from keywords
        String sql = """
                SELECT m.id, m.title, m.content, m.importance_score, m.context, m.created_at,
                       ts_rank(
                           setweight(to_tsvector('english', COALESCE(m.title, '')), 'A') ||
                           setweight(to_tsvector('english', COALESCE(m.content, '')), 'B'),
                           to_tsquery('english', ?)
                       ) as rank
                FROM memories m
                WHERE m.user_id = ?
                  AND m.archived = false
                  AND (
                      to_tsvector('english', COALESCE(m.title, '') || ' ' || COALESCE(m.content, ''))
                      @@ to_tsquery('english', ?)
                      OR LOWER(m.title) LIKE ?
                      OR LOWER(m.content) LIKE ?
                  )
                ORDER BY rank DESC, m.importance_score DESC
                LIMIT ?
                """;

        // For LIKE fallback, try to find a meaningful keyword if possible, otherwise
        // first word
        String likePattern = "%" + question.toLowerCase() + "%";
        if (!keywords.isEmpty()) {
            // Use the longest keyword as the LIKE pattern for better specificity
            String bestKeyword = keywords.stream()
                    .max((s1, s2) -> s1.length() - s2.length())
                    .orElse(question.split("\\s+")[0]);
            likePattern = "%" + bestKeyword.toLowerCase() + "%";
        }

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Memory memory = new Memory();
                memory.setId(rs.getLong("id"));
                memory.setTitle(rs.getString("title"));
                memory.setContent(rs.getString("content"));
                memory.setImportanceScore(rs.getInt("importance_score"));
                var timestamp = rs.getTimestamp("created_at");
                if (timestamp != null) {
                    memory.setCreatedAt(timestamp.toInstant());
                }

                double rank = rs.getDouble("rank");
                double similarity = similarityService.cosineSimilarity(question,
                        memory.getTitle() + " " + memory.getContent());

                return new ScoredMemory(memory, Math.max(rank, similarity));
            }, searchQuery, userId, searchQuery, likePattern, likePattern, limit);
        } catch (Exception e) {
            log.warn("Full-text search failed, falling back to LIKE: {}", e.getMessage());
            return fallbackSearch(question, userId, limit);
        }
    }

    /**
     * Fallback to LIKE search if full-text search fails.
     */
    private List<ScoredMemory> fallbackSearch(String question, String userId, int limit) {
        String pattern = "%" + question.toLowerCase() + "%";

        return jdbcTemplate.query("""
                SELECT id, title, content, importance_score, created_at
                FROM memories
                WHERE user_id = ? AND archived = false
                  AND (LOWER(title) LIKE ? OR LOWER(content) LIKE ?)
                ORDER BY importance_score DESC
                LIMIT ?
                """, (rs, rowNum) -> {
            Memory memory = new Memory();
            memory.setId(rs.getLong("id"));
            memory.setTitle(rs.getString("title"));
            memory.setContent(rs.getString("content"));
            memory.setImportanceScore(rs.getInt("importance_score"));

            double similarity = similarityService.cosineSimilarity(question,
                    memory.getTitle() + " " + memory.getContent());

            return new ScoredMemory(memory, similarity);
        }, userId, pattern, pattern, limit);
    }

    /**
     * Expand context by finding related memories via relationships.
     */
    private List<ScoredMemory> expandContextViaRelationships(List<ScoredMemory> baseMemories, String userId) {
        Set<Long> processedIds = baseMemories.stream()
                .map(m -> m.memory.getId())
                .collect(Collectors.toSet());

        List<ScoredMemory> expanded = new ArrayList<>();

        for (ScoredMemory scored : baseMemories) {
            List<MemoryRelationship> relationships = relationshipRepository.findAllConnected(scored.memory.getId());

            for (MemoryRelationship rel : relationships) {
                Long relatedId = rel.getSourceMemory().getId().equals(scored.memory.getId())
                        ? rel.getTargetMemory().getId()
                        : rel.getSourceMemory().getId();

                if (!processedIds.contains(relatedId)) {
                    processedIds.add(relatedId);
                    memoryRepository.findById(relatedId).ifPresent(related -> {
                        if (related.getUserId().equals(userId) && !related.isArchived()) {
                            // Score based on relationship strength and base memory score
                            double relScore = scored.score * rel.getStrength().doubleValue() * 0.5;
                            expanded.add(new ScoredMemory(related, relScore));
                        }
                    });
                }
            }
        }

        return expanded;
    }

    /**
     * Synthesize a coherent answer from the relevant memories using Gemini LLM.
     */
    private String synthesizeAnswer(String question,
            List<ScoredMemory> relevantMemories,
            List<ScoredMemory> expandedContext) {

        if (relevantMemories.isEmpty()) {
            return "I don't have enough information in your memories to answer that question.";
        }

        // Collect memory contexts
        List<String> memoryContexts = new ArrayList<>();

        // Add direct relevant memories
        for (ScoredMemory scored : relevantMemories) {
            Memory m = scored.memory;
            memoryContexts.add(String.format("Title: %s\nContent: %s\n(Created: %s)",
                    m.getTitle(), m.getContent(), m.getCreatedAt()));
        }

        // Add expanded context (relationships)
        for (ScoredMemory scored : expandedContext) {
            Memory m = scored.memory;
            memoryContexts.add(String.format("[Related] Title: %s\nContent: %s",
                    m.getTitle(), m.getContent()));
        }

        log.info("Sending {} memory contexts to Gemini for synthesis", memoryContexts.size());

        // Use proper LLM for synthesis
        return geminiService.generateAnswer(question, memoryContexts);
    }

    /**
     * Calculate overall confidence in the answer.
     * With LLM, this is less deterministic, but we can base it on retrieval scores.
     */
    private double calculateConfidence(List<ScoredMemory> memories) {
        if (memories.isEmpty())
            return 0.0;

        double avgScore = memories.stream()
                .mapToDouble(m -> m.score)
                .average()
                .orElse(0.0);

        // Boost confidence if we have multiple high-quality matches
        // For LLMs, having good context is key.
        return Math.min(1.0, avgScore);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Inner classes

    public record ScoredMemory(Memory memory, double score) {
    }
}
